package org.fogbow.federatednetwork.controllers;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.log4j.Logger;
import org.fogbow.federatednetwork.CoreRequester;
import org.fogbow.federatednetwork.FederatedNetworkConstants;
import org.fogbow.federatednetwork.FederatedNetworksDB;
import org.fogbow.federatednetwork.ProcessUtil;
import org.fogbow.federatednetwork.exceptions.FederatedComputeNotFoundException;
import org.fogbow.federatednetwork.exceptions.NotEmptyFederatedNetworkException;
import org.fogbow.federatednetwork.exceptions.SubnetAddressesCapacityReachedException;
import org.fogbow.federatednetwork.model.FederatedComputeInstance;
import org.fogbow.federatednetwork.model.FederatedNetwork;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.token.FederationUser;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class FederatedNetworkController {

	private static final Logger LOGGER = Logger.getLogger(FederatedNetworkController.class);

	private static final String DATABASE_FILE_PATH = "federated-networks.db";

	private String permissionFilePath;
	private String agentUser;
	private String agentPublicIp;
	private String agentPrivateIp;

	private FederatedNetworksDB database;

	public FederatedNetworkController(String permissionFilePath, String agentUser, String agentPrivateIp, String agentPublicIp) {
		this(permissionFilePath, agentUser, agentPrivateIp, agentPublicIp, DATABASE_FILE_PATH);
	}

	public FederatedNetworkController(String permissionFilePath, String agentUser, String agentPrivateIp,
	                                  String agentPublicIp, String databaseFilePath) {
		this.permissionFilePath = permissionFilePath;
		this.agentUser = agentUser;
		this.agentPrivateIp = agentPrivateIp;
		this.agentPublicIp = agentPublicIp;

		this.database = new FederatedNetworksDB(databaseFilePath);
	}

	public String create(FederatedNetwork federatedNetwork, FederationUser user) {
		SubnetUtils.SubnetInfo subnetInfo = getSubnetInfo(federatedNetwork.getCidr());

		if (!isValid(subnetInfo)) {
			LOGGER.error("Subnet (" + federatedNetwork.getCidr() + ") invalid");
			return null;
		}

		boolean createdSuccessfully = callFederatedNetworkAgent(federatedNetwork.getCidr(), subnetInfo.getLowAddress());
		if (createdSuccessfully) {
			if (database.addFederatedNetwork(federatedNetwork, user)) {
				return federatedNetwork.getId();
			} else {
				return null;
			}
		}

		return null;
	}

	public boolean callFederatedNetworkAgent(String cidrNotation, String virtualIpAddress) {
		ProcessBuilder builder = new ProcessBuilder("ssh", "-o", "UserKnownHostsFile=/dev/null", "-o",
				"StrictHostKeyChecking=no", "-i", permissionFilePath, agentUser + "@" + agentPublicIp,
				"sudo", "/home/ubuntu/config-ipsec", agentPrivateIp, agentPublicIp, cidrNotation, virtualIpAddress);
		LOGGER.info("Trying to call agent with atts (" + cidrNotation + "): " + builder.command());

		int resultCode = 0;
		try {
			Process process = builder.start();
			LOGGER.info("Trying agent with atts (" + cidrNotation + "). Output : " + ProcessUtil.getOutput(process));
			LOGGER.info("Trying agent with atts (" + cidrNotation + "). Error : " + ProcessUtil.getError(process));
			resultCode = process.waitFor();
			if (resultCode == 0) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("", e);
		}
		LOGGER.error("Is not possible call agent. Process command: " + resultCode);
		return false;
	}

	public Collection<FederatedNetwork> getUserFederatedNetworks(FederationUser user) {
		return database.getUserNetworks(user);
	}

	public FederatedNetwork getFederatedNetwork(String federatedNetworkId, FederationUser user) {
		Collection<FederatedNetwork> allFederatedNetworks = this.getUserFederatedNetworks(user);
		for (FederatedNetwork federatedNetwork : allFederatedNetworks) {
			if (federatedNetwork.getId().equals(federatedNetworkId)) {
				return federatedNetwork;
			}
		}

		return null;
	}

	public void updateFederatedNetworkMembers(FederationUser user, String federatedNetworkId,
	                                          Set<String> membersSet) {
		FederatedNetwork federatedNetwork = this.getFederatedNetwork(federatedNetworkId, user);
		if (federatedNetwork == null) {
			throw new IllegalArgumentException(
					FederatedNetworkConstants.NOT_FOUND_FEDERATED_NETWORK_MESSAGE
							+ federatedNetworkId);
		}
		for (String member : membersSet) {
			federatedNetwork.addFederationNetworkMember(member);
		}

		if (!this.database.addFederatedNetwork(federatedNetwork, user)) {
			throw new IllegalArgumentException(
					FederatedNetworkConstants.CANNOT_UPDATE_FEDERATED_NETWORK_IN_DATABASE
							+ federatedNetworkId);
		}
	}

	public String getCIDRFromFederatedNetwork(String federatedNetworkId, FederationUser user) {
		FederatedNetwork federatedNetwork = this.getFederatedNetwork(federatedNetworkId, user);
		if (federatedNetwork == null) {
			throw new IllegalArgumentException(
					FederatedNetworkConstants.NOT_FOUND_FEDERATED_NETWORK_MESSAGE
							+ federatedNetworkId);
		}
		return federatedNetwork.getCidr();
	}

	public String getPrivateIpFromFederatedNetwork(String federatedNetworkId, String orderId, FederationUser user) throws SubnetAddressesCapacityReachedException {
		LOGGER.info("Getting FN Ip to Order: " + orderId);
		FederatedNetwork federatedNetwork = this.getFederatedNetwork(federatedNetworkId, user);
		if (federatedNetwork == null) {
			throw new IllegalArgumentException(
					FederatedNetworkConstants.NOT_FOUND_FEDERATED_NETWORK_MESSAGE
							+ federatedNetworkId);
		}
		LOGGER.info("FederatedNetwork: " + federatedNetwork.toString());
		String privateIp = null;
		privateIp = federatedNetwork.nextFreeIp(orderId);
		LOGGER.info("FederatedNetwork: " + federatedNetwork.toString());
		if (!this.database.addFederatedNetwork(federatedNetwork, user)) {
			throw new IllegalArgumentException(
					FederatedNetworkConstants.CANNOT_UPDATE_FEDERATED_NETWORK_IN_DATABASE);
		}
		return privateIp;
	}

	public void deleteFederatedNetwork(String federatedNetworkId, FederationUser user) throws NotEmptyFederatedNetworkException {
		LOGGER.info("Initializing delete method, user: " + user + ", federated network id: " + federatedNetworkId);
		FederatedNetwork federatedNetwork = this.getFederatedNetwork(federatedNetworkId, user);
		if (federatedNetwork == null) {
			throw new IllegalArgumentException(
					FederatedNetworkConstants.NOT_FOUND_FEDERATED_NETWORK_MESSAGE
							+ federatedNetworkId);
		}
		LOGGER.info("Trying to delete federated network: " + federatedNetwork.toString());
		// TODO: check if deleteFederatedNetworkFromAgent is false (whether don't find script file, for example).
		if (!federatedNetwork.getComputeIpMap().isEmpty()) {
			throw new NotEmptyFederatedNetworkException();
		}
		if (deleteFederatedNetworkFromAgent(federatedNetwork.getCidr()) == true) {
			LOGGER.info("Successfully deleted federated network: " + federatedNetwork.toString() + " on agent.");
			if (!this.database.delete(federatedNetwork, user)) {
				LOGGER.info("Error to delete federated network: " + federatedNetwork.toString());
				throw new IllegalArgumentException(
						FederatedNetworkConstants.CANNOT_UPDATE_FEDERATED_NETWORK_IN_DATABASE
								+ federatedNetworkId);
			}
		}
	}

	public boolean deleteFederatedNetworkFromAgent(String cidrNotation) {
		ProcessBuilder builder = new ProcessBuilder("ssh", "-o", "UserKnownHostsFile=/dev/null", "-o",
				"StrictHostKeyChecking=no", "-i", permissionFilePath, agentUser + "@" + agentPublicIp,
				"sudo", "/home/ubuntu/remove-network", cidrNotation);
		LOGGER.info("Trying to remove network on agent with atts (" + cidrNotation + "): " + builder.command());

		int resultCode = 0;
		try {
			Process process = builder.start();
			LOGGER.info("Trying remove network on agent with CIDR (" + cidrNotation + "). Output : " + ProcessUtil.getOutput(process));
			LOGGER.info("Trying remove network on agent with CIDR (" + cidrNotation + "). Error : " + ProcessUtil.getError(process));
			resultCode = process.waitFor();
			if (resultCode == 0) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("", e);
		}
		LOGGER.error("Is not possible remove network on agent. Process command: " + resultCode);
		return false;
	}

	public String activateCompute(ComputeOrder computeOrder, String federatedNetworkId, FederationUser federationUser)
			throws SubnetAddressesCapacityReachedException, IOException {
		if (federatedNetworkId != null && !federatedNetworkId.isEmpty()) {
			FederatedNetwork federatedNetwork;
			federatedNetwork = getFederatedNetwork(federatedNetworkId, federationUser);
			String federatedIp = getPrivateIpFromFederatedNetwork(federatedNetworkId, computeOrder.getId(), federationUser);
			computeOrder = FederateComputeUtil.addUserData(computeOrder, federatedIp, agentPublicIp, federatedNetwork.getCidr());
		}
		return CoreRequester.createCompute(computeOrder);
	}

	public ComputeInstance getCompute(String computeId, FederationUser federationUser) throws FederatedComputeNotFoundException {
		ComputeInstance computeInstance = null;
		ComputeInstance compute = CoreRequester.getCompute(computeId);

		final Collection<FederatedNetwork> userNetworks = database.getUserNetworks(federationUser);
		if (!userNetworks.isEmpty()) {
			final String federatedIp = getFederatedIp(computeId, federationUser);
			if (!federatedIp.isEmpty()) {
				computeInstance = new FederatedComputeInstance(compute, federatedIp);
			} else {
				throw new FederatedComputeNotFoundException();
			}
		}

		return computeInstance;
	}

	public void deleteCompute(String computeId, FederationUser federationUser) throws FederatedComputeNotFoundException {
		final Collection<FederatedNetwork> userNetworks = database.getUserNetworks(federationUser);
		for (FederatedNetwork federatedNetwork : userNetworks) {
			federatedNetwork.getComputeIpMap().remove(computeId);
		}

		CoreRequester.deleteCompute(computeId);
	}

	private String getFederatedIp(String computeOrderId, FederationUser federationUser) throws FederatedComputeNotFoundException {
		final Collection<FederatedNetwork> userNetworks = database.getUserNetworks(federationUser);
		String federatedIp = "";
		for (FederatedNetwork federatedNetwork: userNetworks){
			final Map<String, String> orderIpMap = federatedNetwork.getComputeIpMap();
			if (orderIpMap.containsKey(computeOrderId)) {
				federatedIp = orderIpMap.get(computeOrderId);
			}
		}
		return federatedIp;
	}

	private static SubnetUtils.SubnetInfo getSubnetInfo(String cidrNotation) {
		return new SubnetUtils(cidrNotation).getInfo();
	}

	private static boolean isValid(SubnetUtils.SubnetInfo subnetInfo) {
		int lowAddress = subnetInfo.asInteger(subnetInfo.getLowAddress());
		int highAddress = subnetInfo.asInteger(subnetInfo.getHighAddress());
		return highAddress - lowAddress > 1;
	}

}
