package org.eclipse.milo.examples.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.dtd.DataTypeDictionaryManager;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

public class ExampleNamespace extends ManagedNamespaceWithLifecycle {

    public static final String NAMESPACE_URI = "urn:eclipse:milo:atlas";


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DataTypeDictionaryManager dictionaryManager;
  
    Set<Device> devices = new HashSet();
    
	RestClient client;
	String login;
	String psw;
	
	private final SubscriptionModel subscriptionModel;
    private final OpcUaServer server;
    private final UShort namespaceIndex;

    UaFolderNode rootNode;
    String name;
    String uri;
    Map config;
    
    Map<UaVariableNode, Device> nodesToDevices = new HashMap();
    Map<UaVariableNode, Asset> nodesToAssets = new HashMap();
    

    ExampleNamespace(OpcUaServer server) {
        super(server, NAMESPACE_URI);
      
        this.server = server;
    
        subscriptionModel = new SubscriptionModel(server, this);
        dictionaryManager = new DataTypeDictionaryManager(getNodeContext(), NAMESPACE_URI);
		this.namespaceIndex = null;

        getLifecycleManager().addLifecycle(dictionaryManager);
        getLifecycleManager().addLifecycle(subscriptionModel);

        getLifecycleManager().addStartupTask(this::createAndAddNodes);
      
    }
    
    
    @Override
    public void write(WriteContext context, List<WriteValue> writeValues) {
    	super.write(context, writeValues);
    	
    	List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

        for (WriteValue writeValue : writeValues) {
        	UaNode node = getNodeManager().get(writeValue.getNodeId());

            if (node != null) {
                try {
                    node.writeAttribute(
                            new AttributeContext(context),
                            writeValue.getAttributeId(),
                            writeValue.getValue(),
                            writeValue.getIndexRange()
                    );                   
                    
                    NodeId nodeId = node.getNodeId();
    				String identifier = (String) nodeId.getIdentifier();
    				String[] nodes = identifier.split("/");
    				
    				boolean isSaveSuccessful = false;
    				ObjectMapper mapper = new ObjectMapper();     				
    				Object objValue = writeValue.getValue().getValue().getValue();
					NodeId dataType =  ((UaVariableNode) node).getDataType();
					String strNode = "{\""+node.getDisplayName().getText()+"\": "+converter(dataType, objValue)+"}";
					
    				if (nodes[1].equals("Asset")) {
    					Asset tenantAsset = nodesToAssets.get(node);    					
    				    try {
							JsonNode newNode = mapper.readTree(strNode);
							if (nodes[3].equals("Attribute")) {
								isSaveSuccessful = client.saveEntityAttributesV2(tenantAsset.getId(), "SERVER_SCOPE", newNode);
							}else {
								isSaveSuccessful = client.saveEntityTelemetry(tenantAsset.getId(), "SERVER_SCOPE", newNode);
							}
						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}    				    
    				}else {
    					Device tenantDevice = nodesToDevices.get(node);    					
    				    try {
							JsonNode newNode = mapper.readTree(strNode);
							if (nodes[3].equals("Attribute")) {
								isSaveSuccessful = client.saveEntityAttributesV2(tenantDevice.getId(), "SERVER_SCOPE", newNode);
							}else {
								isSaveSuccessful = client.saveEntityTelemetry(tenantDevice.getId(), "SERVER_SCOPE", newNode);	
							}
						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
    				}
    				results.add(isSaveSuccessful ? StatusCode.GOOD : StatusCode.BAD);
                    logger.error(
                            "Wrote value {} to {} attribute of {}",
                            writeValue.getValue().getValue(),
                            AttributeId.from(writeValue.getAttributeId()).map(Object::toString).orElse("unknown"),
                            node.getNodeId());
                } catch (UaException e) {
                    logger.error("Unable to write value={}", writeValue.getValue(), e);
                    results.add(e.getStatusCode());
                }
            } else {
                results.add(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.success(results);
    }
        
    
	@Override
	public void read(ReadContext context, Double maxAge, TimestampsToReturn timestamps,
			List<ReadValueId> readValueIds) {
		super.read(context, maxAge, timestamps, readValueIds);

		List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

		for (ReadValueId readValueId : readValueIds) {
			UaNode node = getNodeManager().get(readValueId.getNodeId());
			if (node != null  && node instanceof UaVariableNode) {
				DataValue value = node.readAttribute(new AttributeContext(context), 
						readValueId.getAttributeId(),
						timestamps, 
						readValueId.getIndexRange(), 
						readValueId.getDataEncoding());
				NodeId nodeId = node.getNodeId();
				String identifier = (String) nodeId.getIdentifier();
				String[] nodes = identifier.split("/");
				
				List<String> keys = new ArrayList();
				keys.add(node.getDisplayName().getText());
				
				if (nodes[1].equals("Asset")) {
					Asset tenantAsset = nodesToAssets.get(node);					
					if (nodes[3].equals("Attribute")) {					
						List<AttributeKvEntry> attributes =  client.getAttributeKvEntries(tenantAsset.getId(), keys);
				    	for(AttributeKvEntry attrKvEntry: attributes) {
				    		((UaVariableNode) node).setValue(new DataValue(new Variant(attrKvEntry.getValue())));
				    	}
					}else {
						List<TsKvEntry> tsKvEntrys = client.getLatestTimeseries(tenantAsset.getId(), keys, true);
						for (TsKvEntry tsKvEntry : tsKvEntrys) {				
							((UaVariableNode) node).setValue(new DataValue(new Variant(tsKvEntry.getValue())));
						}
					}
				}else {
					Device tenantDevice = nodesToDevices.get(node);					
					if (nodes[3].equals("Attribute")) {					
						List<AttributeKvEntry> attributes =  client.getAttributeKvEntries(tenantDevice.getId(), keys);
				    	for(AttributeKvEntry attrKvEntry: attributes) {
				    		((UaVariableNode) node).setValue(new DataValue(new Variant(attrKvEntry.getValue())));
				    	}
					}else {
						List<TsKvEntry> tsKvEntrys = client.getLatestTimeseries(tenantDevice.getId(), keys, true);
						for (TsKvEntry tsKvEntry : tsKvEntrys) {
							((UaVariableNode) node).setValue(new DataValue(new Variant(tsKvEntry.getValue())));
						}
					}
				}				

				results.add(value);
			} else {
				results.add(new DataValue(StatusCodes.Bad_NodeIdUnknown));
			}
		}
		context.success(results);
	}
	
	private void createAndAddNodes() {
		UaFolderNode rootNode = new UaFolderNode(getNodeContext(), newNodeId("Atlas"), newQualifiedName("Atlas"),
				LocalizedText.english("Atlas"));
		getNodeManager().addNode(rootNode);
		rootNode.addReference(new Reference(rootNode.getNodeId(), Identifiers.Organizes,
				Identifiers.ObjectsFolder.expanded(), false));

		NodeId deviceNodeId = newNodeId("Atlas/Device");
		UaFolderNode deviceNode = new UaFolderNode(getNodeContext(), deviceNodeId, newQualifiedName("Device"),
				LocalizedText.english("Device"));
		getNodeManager().addNode(deviceNode);
		rootNode.addOrganizes(deviceNode);

		NodeId assetNodeId = newNodeId("Atlas/Asset");
		UaFolderNode assetNode = new UaFolderNode(getNodeContext(), assetNodeId, newQualifiedName("Asset"),
				LocalizedText.english("Asset"));
		getNodeManager().addNode(assetNode);
		rootNode.addOrganizes(assetNode);

		RestClient client = new RestClient(System.getenv("ATLAS_URL"));
		client.login("devloper1@atlas.local", "vwIyq2vYvlfH9eKo");
		PageData<Device> tenantDevices;
		PageLink pageLink = new PageLink(10);
		do {
			tenantDevices = client.getTenantDevices("", pageLink);
			tenantDevices.getData().forEach(System.out::println);
			for (Device tenantDevice : tenantDevices.getData()) {
				UaFolderNode device = new UaFolderNode(getNodeContext(),
						newNodeId("Atlas/Device/" + tenantDevice.getName()), newQualifiedName(tenantDevice.getName()),
						LocalizedText.english(tenantDevice.getName()));
				getNodeManager().addNode(device);
				deviceNode.addOrganizes(device);

				UaFolderNode attribute = new UaFolderNode(getNodeContext(),
						newNodeId("Atlas/Device/" + tenantDevice.getName() + "/Attribute"),
						newQualifiedName("Attribute"), LocalizedText.english("Attribute"));
				getNodeManager().addNode(attribute);
				device.addOrganizes(attribute);

				List<String> attributeKeys = client.getAttributeKeys(tenantDevice.getId());

				System.out.println(tenantDevice.getName() + "  " + attributeKeys);
				List<AttributeKvEntry> attributes = client.getAttributeKvEntries(tenantDevice.getId(), attributeKeys);
				for (AttributeKvEntry attrKvEntry : attributes) {
					NodeId typeId = getDataType(attrKvEntry.getDataType().name());
					UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
							.setNodeId(newNodeId(
									"Atlas/Device/" + tenantDevice.getName() + "/Attribute/" + attrKvEntry.getKey()))
							.setAccessLevel(AccessLevel.HistoryRead).setUserAccessLevel(AccessLevel.HistoryRead)
							.setBrowseName(newQualifiedName(attrKvEntry.getKey()))
							.setDisplayName(LocalizedText.english(attrKvEntry.getKey())).setDataType(typeId)
							.setTypeDefinition(Identifiers.BaseDataVariableType).setHistorizing(true).build();
					getNodeManager().addNode(node);
					attribute.addOrganizes(node);
					nodesToDevices.put(node, tenantDevice);
				}

				UaFolderNode telemetry = new UaFolderNode(getNodeContext(),
						newNodeId("Atlas/Device/" + tenantDevice.getName() + "/Telemetry"),
						newQualifiedName("Telemetry"), LocalizedText.english("Telemetry"));
				getNodeManager().addNode(telemetry);
				device.addOrganizes(telemetry);
				List<String> timeseriesKeys = client.getTimeseriesKeys(tenantDevice.getId());
				List<TsKvEntry> tsKvEntrys = client.getLatestTimeseries(tenantDevice.getId(), timeseriesKeys, true);
				for (TsKvEntry tsKvEntry : tsKvEntrys) {
					NodeId typeId = getDataType(tsKvEntry.getDataType().name());
					UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
							.setNodeId(newNodeId("Atlas/Device/" + tenantDevice.getName() + "/Telemetry/" + tsKvEntry.getKey()))
							.setAccessLevel(AccessLevel.HistoryRead).setUserAccessLevel(AccessLevel.HistoryRead)
							.setBrowseName(newQualifiedName(tsKvEntry.getKey()))
							.setDisplayName(LocalizedText.english(tsKvEntry.getKey())).setDataType(typeId)
							.setTypeDefinition(Identifiers.BaseDataVariableType).setHistorizing(true).build();

					getNodeManager().addNode(node);
					telemetry.addOrganizes(node);
					nodesToDevices.put(node, tenantDevice);
				}
			}

			pageLink = pageLink.nextPageLink();
		} while (tenantDevices.hasNext());

		PageData<Asset> tenantAsserts;
		pageLink = new PageLink(10);
		do {
			// Fetch all tenant devices using current page link and print each of them
			tenantAsserts = client.getTenantAssets(pageLink, "");
			tenantAsserts.getData().forEach(System.out::println);
			for (Asset tenantAsset : tenantAsserts.getData()) {
				UaFolderNode asset = new UaFolderNode(getNodeContext(),
						newNodeId("Atlas/Asset/" + tenantAsset.getName()), newQualifiedName(tenantAsset.getName()),
						LocalizedText.english(tenantAsset.getName()));
				getNodeManager().addNode(asset);
				assetNode.addOrganizes(asset);

				UaFolderNode attribute = new UaFolderNode(getNodeContext(),
						newNodeId("Atlas/Asset/" + tenantAsset.getName() + "/Attribute"), newQualifiedName("Attribute"),
						LocalizedText.english("Attribute"));
				getNodeManager().addNode(attribute);
				asset.addOrganizes(attribute);

				List<String> attributeKeys = client.getAttributeKeys(tenantAsset.getId());

				System.out.println(tenantAsset.getName() + "  " + attributeKeys);
				List<AttributeKvEntry> attributes = client.getAttributeKvEntries(tenantAsset.getId(), attributeKeys);
				for (AttributeKvEntry attrKvEntry : attributes) {
					NodeId typeId = getDataType(attrKvEntry.getDataType().name());
					UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
							.setNodeId(newNodeId("Atlas/Asset/" + tenantAsset.getName() + "/Attribute/" + attrKvEntry.getKey()))
							.setAccessLevel(AccessLevel.READ_WRITE).setUserAccessLevel(AccessLevel.READ_WRITE)
							.setBrowseName(newQualifiedName(attrKvEntry.getKey()))
							.setDisplayName(LocalizedText.english(attrKvEntry.getKey())).setDataType(typeId)
							.setTypeDefinition(Identifiers.BaseDataVariableType).setHistorizing(true).build();
					getNodeManager().addNode(node);
					attribute.addOrganizes(node);
					nodesToAssets.put(node, tenantAsset);
				}

				UaFolderNode telemetry = new UaFolderNode(getNodeContext(),
						newNodeId("Atlas/Asset/" + tenantAsset.getName() + "/Telemetry"), 
						newQualifiedName("Telemetry"),
						LocalizedText.english("Telemetry"));
				getNodeManager().addNode(telemetry);
				asset.addOrganizes(telemetry);
				List<String> timeseriesKeys = client.getTimeseriesKeys(tenantAsset.getId());
				List<TsKvEntry> tsKvEntrys = client.getLatestTimeseries(tenantAsset.getId(), timeseriesKeys, true);
				for (TsKvEntry tsKvEntry : tsKvEntrys) {
					NodeId typeId = getDataType(tsKvEntry.getDataType().name());
					UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
							.setNodeId(newNodeId("Atlas/Asset/" + tenantAsset.getName() + "/Telemetry/" + tsKvEntry.getKey()))
							.setAccessLevel(AccessLevel.HISTORY_READ_ONLY)
							.setUserAccessLevel(AccessLevel.HISTORY_READ_ONLY)
							.setBrowseName(newQualifiedName(tsKvEntry.getKey()))
							.setDisplayName(LocalizedText.english(tsKvEntry.getKey())).setDataType(typeId)
							.setTypeDefinition(Identifiers.BaseDataVariableType).setHistorizing(true).build();

					getNodeManager().addNode(node);
					telemetry.addOrganizes(node);
					nodesToAssets.put(node, tenantAsset);
				}
			}

			pageLink = pageLink.nextPageLink();
		} while (tenantAsserts.hasNext());
	}
        
    private Object converter(NodeId dataType, Object value) {
		if (dataType.equals(Identifiers.String)) {
			return value + "";
		} else if (dataType.equals(Identifiers.Int64)) {
			return (long) value;
		} else if (dataType.equals(Identifiers.Double)) {
			return (double) value;
		} else if (dataType.equals(Identifiers.Boolean)) {
			return (boolean)value;
		} 
		return null;
	}
    
    
	private NodeId getDataType(String type) {
		if (type.equals("STRING")) {
			return Identifiers.String;
		} else if (type.equals("LONG")) {
			return Identifiers.Int64;
		} else if (type.equals("DOUBLE")) {
			return Identifiers.Double;
		} else if (type.equals("BOOLEAN")) {
			return Identifiers.Boolean;
		} else if (type.equals("JSON")) {
			return Identifiers.String;
		}
		return null;
	}
    

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }
}
