/*
 * Copyright (c) 2022 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.client.dtd;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.DataTypeTreeBuilder;
import org.eclipse.milo.opcua.sdk.client.DataTypeTreeSessionInitializer;
import org.eclipse.milo.opcua.sdk.client.OpcUaSession;
import org.eclipse.milo.opcua.sdk.client.dtd.DataTypeDictionaryReader2.TypeDictionaryInfo;
import org.eclipse.milo.opcua.sdk.client.session.SessionFsm;
import org.eclipse.milo.opcua.sdk.core.types.DataTypeTree;
import org.eclipse.milo.opcua.stack.client.UaStackClient;
import org.eclipse.milo.opcua.stack.core.serialization.codecs.OpcUaBinaryDataTypeCodec;
import org.eclipse.milo.opcua.stack.core.types.DataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.OpcUaBinaryDataTypeDictionary;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.util.Unit;
import org.opcfoundation.opcua.binaryschema.StructuredType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTypeDictionarySessionInitializer2 implements SessionFsm.SessionInitializer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CodecFactory codecFactory;

    public DataTypeDictionarySessionInitializer2(CodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    @Override
    public CompletableFuture<Unit> initialize(UaStackClient client, OpcUaSession session) {
        logger.debug("SessionInitializer: DataTypeDictionary");

        String treeKey = DataTypeTreeSessionInitializer.SESSION_ATTRIBUTE_KEY;

        Object dataTypeTree = session.getAttribute(treeKey);

        if (dataTypeTree instanceof DataTypeTree) {
            return initialize(client, session, (DataTypeTree) dataTypeTree);
        } else {
            return DataTypeTreeBuilder.buildAsync(client, session).thenCompose(tree -> {
                session.setAttribute(treeKey, tree);

                return initialize(client, session, tree);
            });
        }
    }

    private CompletableFuture<Unit> initialize(UaStackClient client, OpcUaSession session, DataTypeTree dataTypeTree) {
        var dictionaryReader = new DataTypeDictionaryReader2(client, session);

        return dictionaryReader.readDataTypeDictionaries()
            .thenAccept(typeDictionaryInfos -> {
                    DataTypeManager dataTypeManager = client.getDynamicDataTypeManager();

                    for (TypeDictionaryInfo typeDictionaryInfo : typeDictionaryInfos) {
                        var dictionary = new OpcUaBinaryDataTypeDictionary(
                            typeDictionaryInfo.typeDictionary.getTargetNamespace()
                        );

                        Map<String, StructuredType> structuredTypes = new HashMap<>();

                        typeDictionaryInfo.typeDictionary.getOpaqueTypeOrEnumeratedTypeOrStructuredType()
                            .stream()
                            .filter(typeDescription -> typeDescription instanceof StructuredType)
                            .map(StructuredType.class::cast)
                            .forEach(structuredType -> structuredTypes.put(structuredType.getName(), structuredType));

                        typeDictionaryInfo.structEncodingInfos.forEach(structEncodingInfo -> {
                            StructuredType structuredType = structuredTypes.get(structEncodingInfo.description);

                            OpcUaBinaryDataTypeCodec codec = codecFactory.createCodec(
                                structEncodingInfo.dataTypeId,
                                structuredType,
                                dataTypeTree
                            );

                            dictionary.registerStructCodec(
                                codec,
                                structEncodingInfo.description,
                                structEncodingInfo.dataTypeId,
                                structEncodingInfo.encodingId
                            );
                        });

                        dataTypeManager.registerBinaryTypeDictionary(dictionary);
                    }
                }
            )
            .thenApply(v -> Unit.VALUE)
            .exceptionally(ex -> {
                logger.warn("SessionInitializer: DataTypeDictionary", ex);
                return Unit.VALUE;
            });
    }

    public interface CodecFactory {

        OpcUaBinaryDataTypeCodec createCodec(
            NodeId dataTypeId, StructuredType structuredType, DataTypeTree dataTypeTree);

    }

}
