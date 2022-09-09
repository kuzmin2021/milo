package org.eclipse.milo.opcua.stack.core.types;

import org.eclipse.milo.opcua.stack.core.serialization.codecs.DataTypeCodec;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.jetbrains.annotations.Nullable;

public interface DataTypeManager {

    void registerEnumType(NodeId dataTypeId, DataTypeCodec codec);

    void registerStructType(
        NodeId dataTypeId,
        DataTypeCodec codec,
        @Nullable NodeId binaryEncodingId,
        @Nullable NodeId xmlEncodingId,
        @Nullable NodeId jsonEncodingId
    );

    @Nullable DataTypeCodec getEnumCodec(NodeId dataTypeId);

    @Nullable DataTypeCodec getStructCodec(NodeId encodingId);

    @Nullable DataTypeCodec getStructCodec(QualifiedName encodingName, NodeId dataTypeId);

    @Nullable NodeId getBinaryEncodingId(NodeId dataTypeId);

    @Nullable NodeId getXmlEncodingId(NodeId dataTypeId);

    @Nullable NodeId getJsonEncodingId(NodeId dataTypeId);

    /**
     * Register a {@link OpcUaBinaryDataTypeDictionary} and all the {@link DataTypeCodec}s it contains.
     *
     * @param dataTypeDictionary the {@link DataTypeDictionary} to register.
     */
    void registerBinaryTypeDictionary(OpcUaBinaryDataTypeDictionary dataTypeDictionary);

    /**
     * Get a registered {@link OpcUaBinaryDataTypeDictionary} by its namespace URI.
     *
     * @param namespaceUri the namespace URI the dictionary is registered under.
     * @return the {@link DataTypeDictionary} registered under {@code namespaceUri}.
     */
    @Nullable OpcUaBinaryDataTypeDictionary getBinaryDataTypeDictionary(String namespaceUri);

    /**
     * Register a {@link OpcUaXmlDataTypeDictionary} and all the {@link DataTypeCodec}s it contains.
     *
     * @param dataTypeDictionary the {@link OpcUaXmlDataTypeDictionary} to register.
     */
    void registerXmlTypeDictionary(OpcUaXmlDataTypeDictionary dataTypeDictionary);

    /**
     * Get a registered {@link OpcUaXmlDataTypeDictionary} by its namespace URI.
     *
     * @param namespaceUri the namespace URI the dictionary is registered under.
     * @return the {@link OpcUaXmlDataTypeDictionary} registered under {@code namespaceUri}.
     */
    @Nullable OpcUaXmlDataTypeDictionary getXmlDataTypeDictionary(String namespaceUri);

}
