/*
 * Copyright (c) 2022 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.stack.core.types.structured;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.encoding.GenericDataTypeCodec;
import org.eclipse.milo.opcua.stack.core.encoding.UaDecoder;
import org.eclipse.milo.opcua.stack.core.encoding.UaEncoder;
import org.eclipse.milo.opcua.stack.core.types.UaResponseMessageType;
import org.eclipse.milo.opcua.stack.core.types.builtin.DiagnosticInfo;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.StructureType;

/**
 * @see <a href="https://reference.opcfoundation.org/v105/Core/docs/Part4/5.12.6/#5.12.6.2">https://reference.opcfoundation.org/v105/Core/docs/Part4/5.12.6/#5.12.6.2</a>
 */
@EqualsAndHashCode(
    callSuper = false
)
@SuperBuilder
@ToString
public class DeleteMonitoredItemsResponse extends Structure implements UaResponseMessageType {
    public static final ExpandedNodeId TYPE_ID = ExpandedNodeId.parse("ns=0;i=782");

    public static final ExpandedNodeId BINARY_ENCODING_ID = ExpandedNodeId.parse("i=784");

    public static final ExpandedNodeId XML_ENCODING_ID = ExpandedNodeId.parse("i=783");

    public static final ExpandedNodeId JSON_ENCODING_ID = ExpandedNodeId.parse("i=15336");

    private final ResponseHeader responseHeader;

    private final StatusCode[] results;

    private final DiagnosticInfo[] diagnosticInfos;

    public DeleteMonitoredItemsResponse(ResponseHeader responseHeader, StatusCode[] results,
                                        DiagnosticInfo[] diagnosticInfos) {
        this.responseHeader = responseHeader;
        this.results = results;
        this.diagnosticInfos = diagnosticInfos;
    }

    @Override
    public ExpandedNodeId getTypeId() {
        return TYPE_ID;
    }

    @Override
    public ExpandedNodeId getBinaryEncodingId() {
        return BINARY_ENCODING_ID;
    }

    @Override
    public ExpandedNodeId getXmlEncodingId() {
        return XML_ENCODING_ID;
    }

    @Override
    public ExpandedNodeId getJsonEncodingId() {
        return JSON_ENCODING_ID;
    }

    public ResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public StatusCode[] getResults() {
        return results;
    }

    public DiagnosticInfo[] getDiagnosticInfos() {
        return diagnosticInfos;
    }

    public static StructureDefinition definition(NamespaceTable namespaceTable) {
        return new StructureDefinition(
            new NodeId(0, 784),
            new NodeId(0, 22),
            StructureType.Structure,
            new StructureField[]{
                new StructureField("ResponseHeader", LocalizedText.NULL_VALUE, new NodeId(0, 392), -1, null, UInteger.valueOf(0), false),
                new StructureField("Results", LocalizedText.NULL_VALUE, new NodeId(0, 19), 1, null, UInteger.valueOf(0), false),
                new StructureField("DiagnosticInfos", LocalizedText.NULL_VALUE, new NodeId(0, 25), 1, null, UInteger.valueOf(0), false)
            }
        );
    }

    public static final class Codec extends GenericDataTypeCodec<DeleteMonitoredItemsResponse> {
        @Override
        public Class<DeleteMonitoredItemsResponse> getType() {
            return DeleteMonitoredItemsResponse.class;
        }

        @Override
        public DeleteMonitoredItemsResponse decodeType(EncodingContext context,
                                                       UaDecoder decoder) {
            ResponseHeader responseHeader = (ResponseHeader) decoder.decodeStruct("ResponseHeader", ResponseHeader.TYPE_ID);
            StatusCode[] results = decoder.decodeStatusCodeArray("Results");
            DiagnosticInfo[] diagnosticInfos = decoder.decodeDiagnosticInfoArray("DiagnosticInfos");
            return new DeleteMonitoredItemsResponse(responseHeader, results, diagnosticInfos);
        }

        @Override
        public void encodeType(EncodingContext context, UaEncoder encoder,
                               DeleteMonitoredItemsResponse value) {
            encoder.encodeStruct("ResponseHeader", value.getResponseHeader(), ResponseHeader.TYPE_ID);
            encoder.encodeStatusCodeArray("Results", value.getResults());
            encoder.encodeDiagnosticInfoArray("DiagnosticInfos", value.getDiagnosticInfos());
        }
    }
}
