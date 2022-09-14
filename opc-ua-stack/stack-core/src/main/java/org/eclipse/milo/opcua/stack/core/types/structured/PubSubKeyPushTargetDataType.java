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
import org.eclipse.milo.opcua.stack.core.types.UaStructuredType;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.StructureType;

/**
 * @see <a href="https://reference.opcfoundation.org/v105/Core/docs/Part14/6.2.12/#6.2.12.3">https://reference.opcfoundation.org/v105/Core/docs/Part14/6.2.12/#6.2.12.3</a>
 */
@EqualsAndHashCode(
    callSuper = false
)
@SuperBuilder
@ToString
public class PubSubKeyPushTargetDataType extends Structure implements UaStructuredType {
    public static final ExpandedNodeId TYPE_ID = ExpandedNodeId.parse("ns=0;i=25270");

    public static final ExpandedNodeId BINARY_ENCODING_ID = ExpandedNodeId.parse("i=25530");

    public static final ExpandedNodeId XML_ENCODING_ID = ExpandedNodeId.parse("i=25546");

    public static final ExpandedNodeId JSON_ENCODING_ID = ExpandedNodeId.parse("i=25562");

    private final String applicationUri;

    private final String[] pushTargetFolder;

    private final String endpointUrl;

    private final String securityPolicyUri;

    private final UserTokenPolicy userTokenType;

    private final UShort requestedKeyCount;

    private final Double retryInterval;

    private final KeyValuePair[] pushTargetProperties;

    private final String[] securityGroups;

    public PubSubKeyPushTargetDataType(String applicationUri, String[] pushTargetFolder,
                                       String endpointUrl, String securityPolicyUri, UserTokenPolicy userTokenType,
                                       UShort requestedKeyCount, Double retryInterval, KeyValuePair[] pushTargetProperties,
                                       String[] securityGroups) {
        this.applicationUri = applicationUri;
        this.pushTargetFolder = pushTargetFolder;
        this.endpointUrl = endpointUrl;
        this.securityPolicyUri = securityPolicyUri;
        this.userTokenType = userTokenType;
        this.requestedKeyCount = requestedKeyCount;
        this.retryInterval = retryInterval;
        this.pushTargetProperties = pushTargetProperties;
        this.securityGroups = securityGroups;
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

    public String getApplicationUri() {
        return applicationUri;
    }

    public String[] getPushTargetFolder() {
        return pushTargetFolder;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getSecurityPolicyUri() {
        return securityPolicyUri;
    }

    public UserTokenPolicy getUserTokenType() {
        return userTokenType;
    }

    public UShort getRequestedKeyCount() {
        return requestedKeyCount;
    }

    public Double getRetryInterval() {
        return retryInterval;
    }

    public KeyValuePair[] getPushTargetProperties() {
        return pushTargetProperties;
    }

    public String[] getSecurityGroups() {
        return securityGroups;
    }

    public static StructureDefinition definition(NamespaceTable namespaceTable) {
        return new StructureDefinition(
            new NodeId(0, 25530),
            new NodeId(0, 22),
            StructureType.Structure,
            new StructureField[]{
                new StructureField("ApplicationUri", LocalizedText.NULL_VALUE, new NodeId(0, 12), -1, null, UInteger.valueOf(0), false),
                new StructureField("PushTargetFolder", LocalizedText.NULL_VALUE, new NodeId(0, 12), 1, null, UInteger.valueOf(0), false),
                new StructureField("EndpointUrl", LocalizedText.NULL_VALUE, new NodeId(0, 12), -1, null, UInteger.valueOf(0), false),
                new StructureField("SecurityPolicyUri", LocalizedText.NULL_VALUE, new NodeId(0, 12), -1, null, UInteger.valueOf(0), false),
                new StructureField("UserTokenType", LocalizedText.NULL_VALUE, new NodeId(0, 304), -1, null, UInteger.valueOf(0), false),
                new StructureField("RequestedKeyCount", LocalizedText.NULL_VALUE, new NodeId(0, 5), -1, null, UInteger.valueOf(0), false),
                new StructureField("RetryInterval", LocalizedText.NULL_VALUE, new NodeId(0, 290), -1, null, UInteger.valueOf(0), false),
                new StructureField("PushTargetProperties", LocalizedText.NULL_VALUE, new NodeId(0, 14533), 1, null, UInteger.valueOf(0), false),
                new StructureField("SecurityGroups", LocalizedText.NULL_VALUE, new NodeId(0, 12), 1, null, UInteger.valueOf(0), false)
            }
        );
    }

    public static final class Codec extends GenericDataTypeCodec<PubSubKeyPushTargetDataType> {
        @Override
        public Class<PubSubKeyPushTargetDataType> getType() {
            return PubSubKeyPushTargetDataType.class;
        }

        @Override
        public PubSubKeyPushTargetDataType decodeType(EncodingContext context, UaDecoder decoder) {
            String applicationUri = decoder.decodeString("ApplicationUri");
            String[] pushTargetFolder = decoder.decodeStringArray("PushTargetFolder");
            String endpointUrl = decoder.decodeString("EndpointUrl");
            String securityPolicyUri = decoder.decodeString("SecurityPolicyUri");
            UserTokenPolicy userTokenType = (UserTokenPolicy) decoder.decodeStruct("UserTokenType", UserTokenPolicy.TYPE_ID);
            UShort requestedKeyCount = decoder.decodeUInt16("RequestedKeyCount");
            Double retryInterval = decoder.decodeDouble("RetryInterval");
            KeyValuePair[] pushTargetProperties = (KeyValuePair[]) decoder.decodeStructArray("PushTargetProperties", KeyValuePair.TYPE_ID);
            String[] securityGroups = decoder.decodeStringArray("SecurityGroups");
            return new PubSubKeyPushTargetDataType(applicationUri, pushTargetFolder, endpointUrl, securityPolicyUri, userTokenType, requestedKeyCount, retryInterval, pushTargetProperties, securityGroups);
        }

        @Override
        public void encodeType(EncodingContext context, UaEncoder encoder,
                               PubSubKeyPushTargetDataType value) {
            encoder.encodeString("ApplicationUri", value.getApplicationUri());
            encoder.encodeStringArray("PushTargetFolder", value.getPushTargetFolder());
            encoder.encodeString("EndpointUrl", value.getEndpointUrl());
            encoder.encodeString("SecurityPolicyUri", value.getSecurityPolicyUri());
            encoder.encodeStruct("UserTokenType", value.getUserTokenType(), UserTokenPolicy.TYPE_ID);
            encoder.encodeUInt16("RequestedKeyCount", value.getRequestedKeyCount());
            encoder.encodeDouble("RetryInterval", value.getRetryInterval());
            encoder.encodeStructArray("PushTargetProperties", value.getPushTargetProperties(), KeyValuePair.TYPE_ID);
            encoder.encodeStringArray("SecurityGroups", value.getSecurityGroups());
        }
    }
}
