package org.fogbow.federatednetwork;

import org.apache.commons.net.util.SubnetUtils;
import org.fogbow.federatednetwork.datastore.DatabaseManager;
import org.fogbow.federatednetwork.exceptions.*;
import org.fogbow.federatednetwork.model.FederatedComputeInstance;
import org.fogbow.federatednetwork.model.FederatedComputeOrder;
import org.fogbow.federatednetwork.model.FederatedNetworkOrder;
import org.fogbow.federatednetwork.utils.AgentCommunicatorUtil;
import org.fogbow.federatednetwork.utils.FederateComputeUtil;
import org.fogbow.federatednetwork.utils.FederatedNetworkUtil;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.models.InstanceStatus;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AgentCommunicatorUtil.class, FederatedNetworkUtil.class, FederateComputeUtil.class, DatabaseManager.class})
public class OrderControllerTest extends BaseUnitTest {

    private String FEDERATED_NETWORK_ID = "fake-id";
    private String FEDERATED_COMPUTE_ID = "fake-compute-id";

    Properties properties;
    FederationUser user;
    OrderController orderController;

    @Before
    public void setUp() throws InvalidParameterException {
        properties = super.setProperties();
        orderController = spy(new OrderController(properties));
        Map<String, String> fedUserAttrs = new HashMap<>();
        fedUserAttrs.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        user = new FederationUser("fake-user-id", fedUserAttrs);
    }

    //test case: Tests if the activation order made in orderController will call the expected methods
    @Test
    public void testActivatingFederatedNetwork() throws InvalidCidrException, AgentCommucationException {
        //set up
        String fakeCidr = "10.10.10.0/24";
        SubnetUtils.SubnetInfo fakeSubnetInfo = new SubnetUtils(fakeCidr).getInfo();
        FederationUser user = mock(FederationUser.class);
        FederatedNetworkOrder federatedNetworkOrder = spy(new FederatedNetworkOrder());
        federatedNetworkOrder.setId(FEDERATED_NETWORK_ID);
        federatedNetworkOrder.setCidrNotation(fakeCidr);
        doNothing().when(federatedNetworkOrder).setCachedInstanceState(InstanceState.READY);
        doNothing().when(federatedNetworkOrder).setOrderState(OrderState.FULFILLED);
        PowerMockito.mockStatic(FederatedNetworkUtil.class);
        BDDMockito.given(FederatedNetworkUtil.getSubnetInfo(anyString())).willReturn(fakeSubnetInfo);
        BDDMockito.given(FederatedNetworkUtil.isSubnetValid(any(SubnetUtils.SubnetInfo.class))).willReturn(true);
        PowerMockito.mockStatic(AgentCommunicatorUtil.class);
        BDDMockito.given(AgentCommunicatorUtil.createFederatedNetwork(anyString(), anyString(), any(Properties.class))).willReturn(true);
        // exercise
        String returnedId = orderController.activateFederatedNetwork(federatedNetworkOrder, user);
        //verify
        verify(federatedNetworkOrder, times(1)).setCachedInstanceState(InstanceState.READY);
        verify(federatedNetworkOrder, times(1)).setOrderState(OrderState.FULFILLED);
        assertEquals(FEDERATED_NETWORK_ID, returnedId);
    }

    //test case: Tests if any error in communication with agent, will throw an exception
    @Test
    public void testAgentCommunicationError() throws InvalidCidrException {
        //set up
        String fakeCidr = "10.10.10.0/24";
        SubnetUtils.SubnetInfo fakeSubnetInfo = new SubnetUtils(fakeCidr).getInfo();
        FederationUser user = mock(FederationUser.class);
        FederatedNetworkOrder federatedNetworkOrder = spy(new FederatedNetworkOrder());
        federatedNetworkOrder.setId(FEDERATED_NETWORK_ID);
        federatedNetworkOrder.setCidrNotation(fakeCidr);
        doNothing().when(federatedNetworkOrder).setCachedInstanceState(InstanceState.READY);
        PowerMockito.mockStatic(FederatedNetworkUtil.class);
        BDDMockito.given(FederatedNetworkUtil.getSubnetInfo(anyString())).willReturn(fakeSubnetInfo);
        BDDMockito.given(FederatedNetworkUtil.isSubnetValid(any(SubnetUtils.SubnetInfo.class))).willReturn(true);
        doNothing().when(federatedNetworkOrder).setOrderState(OrderState.FULFILLED);

        PowerMockito.mockStatic(AgentCommunicatorUtil.class);
        BDDMockito.given(AgentCommunicatorUtil.createFederatedNetwork(anyString(), anyString(), any(Properties.class))).willReturn(false);
        // exercise
        try {
            orderController.activateFederatedNetwork(federatedNetworkOrder, user);
            fail();
        } catch (AgentCommucationException e) {
            //verify
            verify(federatedNetworkOrder, times(0)).setCachedInstanceState(InstanceState.READY);
            verify(federatedNetworkOrder, times(0)).setOrderState(OrderState.FULFILLED);
        }
    }

    //test case: Tests that can retrieve a federated network stored into activeFederatedNetwork.
    @Test
    public void testGetFederatedNetwork() {
        //set up
        FederatedNetworkOrder federatedNetwork = mock(FederatedNetworkOrder.class);
        Map<String, FederatedNetworkOrder> fakeActiveFederatedNetworks = new ConcurrentHashMap<>();
        when(federatedNetwork.getFederationUser()).thenReturn(user);
        fakeActiveFederatedNetworks.put(FEDERATED_NETWORK_ID, federatedNetwork);
        orderController.setActiveFederatedNetworks(fakeActiveFederatedNetworks);
        //exercise
        try {
            FederatedNetworkOrder returnedOrder = orderController.getFederatedNetwork(FEDERATED_NETWORK_ID, user);
            //verify
            assertEquals(federatedNetwork, returnedOrder);
        } catch (FederatedNetworkNotFoundException e) {
            fail();
        }
    }

    //test case: This test check if a federated network that can't be found, this get operation should throw a FederatedNetworkNotFoundException
    @Test
    public void testGetNotExistentFederatedNetwork() {
        //exercise
        try {
            FederatedNetworkOrder returnedOrder = orderController.getFederatedNetwork(FEDERATED_NETWORK_ID, user);
            fail();
        } catch (FederatedNetworkNotFoundException e) {
            //verify
        }
    }

    //test case: Tests if a delete operation deletes federatedNetwork from activeFederatedNetworks.
    @Test
    public void testDeleteFederatedNetwork() throws FederatedNetworkNotFoundException, AgentCommucationException {
        //set up
        FederatedNetworkOrder federatedNetwork = mock(FederatedNetworkOrder.class);
        Map<String, FederatedNetworkOrder> fakeActiveFederatedNetworks = new ConcurrentHashMap<>();
        when(federatedNetwork.getFederationUser()).thenReturn(user);
        fakeActiveFederatedNetworks.put(FEDERATED_NETWORK_ID, federatedNetwork);
        orderController.setActiveFederatedNetworks(fakeActiveFederatedNetworks);

        PowerMockito.mockStatic(AgentCommunicatorUtil.class);
        BDDMockito.given(AgentCommunicatorUtil.deleteFederatedNetwork(anyString(), any(Properties.class))).willReturn(true);
        try {
            //exercise
            orderController.deleteFederatedNetwork(FEDERATED_NETWORK_ID, user);
            //verify
        } catch (NotEmptyFederatedNetworkException e) {
            fail();
        }
        try {
            //exercise
            FederatedNetworkOrder returnedOrder = orderController.getFederatedNetwork(FEDERATED_NETWORK_ID, user);
            fail();
        } catch (FederatedNetworkNotFoundException e) {
            //verify
        }
        verify(federatedNetwork, times(1)).setOrderState(OrderState.DEACTIVATED);
        assertNull(orderController.getActiveFederatedNetworks().get(FEDERATED_NETWORK_ID));
    }

    //test case: This test check if a delete in nonexistent federatedNetwork will throw a FederatedNetworkNotFoundException
    @Test
    public void testDeleteNonExistentFederatedNetwork() throws NotEmptyFederatedNetworkException, AgentCommucationException {
        //set up
        try {
            //exercise
            orderController.deleteFederatedNetwork(FEDERATED_NETWORK_ID, user);
            fail();
        } catch (FederatedNetworkNotFoundException e) {
            //verify
        }
    }

    //test case: This test check if an error communicating with agent will throw an AgentCommucationException
    @Test
    public void testErrorInAgentCommunication() throws FederatedNetworkNotFoundException, NotEmptyFederatedNetworkException {
        //set up
        FederatedNetworkOrder federatedNetwork = mock(FederatedNetworkOrder.class);
        Map<String, FederatedNetworkOrder> fakeActiveFederatedNetworks = new ConcurrentHashMap<>();
        federatedNetwork.setId(FEDERATED_NETWORK_ID);
        when(federatedNetwork.getFederationUser()).thenReturn(user);
        fakeActiveFederatedNetworks.put(FEDERATED_NETWORK_ID, federatedNetwork);
        orderController.setActiveFederatedNetworks(fakeActiveFederatedNetworks);

        PowerMockito.mockStatic(AgentCommunicatorUtil.class);
        BDDMockito.given(AgentCommunicatorUtil.deleteFederatedNetwork(anyString(), any(Properties.class))).willReturn(false);
        try {
            //exercise
            orderController.deleteFederatedNetwork(FEDERATED_NETWORK_ID, user);
            fail();
        } catch (AgentCommucationException e) {
            //verify
        }
    }

    //test case: Tests if get all federated networks will return correctly
    @Test
    public void testGetFederatedNetworks() {
        //set up
        FederatedNetworkOrder federatedNetwork = mock(FederatedNetworkOrder.class);
        FederatedNetworkOrder federatedNetwork2 = mock(FederatedNetworkOrder.class);
        String federatedNetworkId2 = FEDERATED_NETWORK_ID + 2;
        Map<String, FederatedNetworkOrder> fakeActiveFederatedNetworks = new ConcurrentHashMap<>();
        when(federatedNetwork.getId()).thenReturn(FEDERATED_NETWORK_ID);
        when(federatedNetwork2.getId()).thenReturn(federatedNetworkId2);
        when(federatedNetwork.getFederationUser()).thenReturn(user);
        when(federatedNetwork2.getFederationUser()).thenReturn(user);
        when(federatedNetwork.getOrderState()).thenReturn(OrderState.FULFILLED);
        when(federatedNetwork2.getOrderState()).thenReturn(OrderState.FULFILLED);
        fakeActiveFederatedNetworks.put(FEDERATED_NETWORK_ID, federatedNetwork);
        fakeActiveFederatedNetworks.put(federatedNetworkId2, federatedNetwork2);
        orderController.setActiveFederatedNetworks(fakeActiveFederatedNetworks);
        //exercise
        List<InstanceStatus> federatedNetworks = new ArrayList<>(orderController.getUserFederatedNetworksStatus(user));
        //verify
        assertEquals(2, federatedNetworks.size());
        assertEquals(FEDERATED_NETWORK_ID, federatedNetworks.get(0).getInstanceId());
        assertEquals(federatedNetworkId2, federatedNetworks.get(1).getInstanceId());
    }

    //test case: Tests if get all in an Empty federated networks list will return correctly
    @Test
    public void testGetEmptyListOfFederatedNetworks() {
        //exercise
        List<InstanceStatus> federatedNetworks = new ArrayList<>(orderController.getUserFederatedNetworksStatus(user));
        //verify
        assertEquals(0, federatedNetworks.size());
    }

    // compute tests


    //test case: Tests if to add a new federated compute, orderController makes the correct calls to the collaborators.
    @Test
    public void testAddFederatedCompute() throws FederatedNetworkNotFoundException, InvalidCidrException,
            SubnetAddressesCapacityReachedException, IOException {
        //set up
        String cidr = "10.10.10.0/24";
        Set<String> allowedMembers = new HashSet<>();
        Queue<String> freedIps = new LinkedList<>();
        List<String> computesIp = new ArrayList<>();
        FederatedNetworkOrder federatedNetwork = spy(new FederatedNetworkOrder(FEDERATED_NETWORK_ID, user, cidr, "test",
                allowedMembers, 1, freedIps, computesIp));
        Map<String, FederatedNetworkOrder> fakeActiveFederatedNetworks = new ConcurrentHashMap<>();
        fakeActiveFederatedNetworks.put(FEDERATED_NETWORK_ID, federatedNetwork);
        orderController.setActiveFederatedNetworks(fakeActiveFederatedNetworks);

        String federatedIp = "10.10.10.2";
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setId(FEDERATED_COMPUTE_ID);
        FederatedComputeOrder federatedCompute = spy(new FederatedComputeOrder(FEDERATED_NETWORK_ID, "", computeOrder));

        PowerMockito.mockStatic(FederatedNetworkUtil.class);
        BDDMockito.given(FederatedNetworkUtil.getFreeIpForCompute(federatedNetwork)).willReturn(federatedIp);
        PowerMockito.mockStatic(FederateComputeUtil.class);
        UserData fakeUserData = new UserData("", CloudInitUserDataBuilder.FileType.SHELL_SCRIPT);
        BDDMockito.given(FederateComputeUtil.addUserData(any(ComputeOrder.class), anyString(), anyString(), anyString(),
                anyString())).willReturn(createComputeWithUserData(computeOrder, fakeUserData));
        //exercise
        orderController.addFederationUserDataIfApplied(federatedCompute, user);
        //verify
        verify(federatedCompute, times(1)).setFederatedIp(federatedIp);
        PowerMockito.verifyStatic(FederatedNetworkUtil.class, times(1));
        FederatedNetworkUtil.getFreeIpForCompute(federatedNetwork);
        PowerMockito.verifyStatic(FederateComputeUtil.class, times(1));
        FederateComputeUtil.addUserData(any(ComputeOrder.class), anyString(), anyString(), anyString(),
                anyString());
        assertNotEquals(computeOrder.getUserData(), orderController.getActiveFederatedNetworks().get(FEDERATED_NETWORK_ID));
    }

    //test case: This test expects a FederatedNetworkException, since will be given a nonexistent federatedNetwork id
    @Test
    public void testAddComputeFederatedWithNonexistentNetwork() throws InvalidCidrException,
            SubnetAddressesCapacityReachedException, IOException {
        //set up
        String nonexistentId = "nonexistent-id";
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setId(FEDERATED_COMPUTE_ID);
        FederatedComputeOrder federatedCompute = spy(new FederatedComputeOrder(nonexistentId, "", computeOrder));
        try {
            //exercise
            orderController.addFederationUserDataIfApplied(federatedCompute, user);
            fail();
        } catch (FederatedNetworkNotFoundException e) {
            //verify
        }
    }

    //test case: Tests if get all in an empty federated networks list will return the same computeOrder given as input.
    @Test
    public void testAddComputeNotFederated() throws InvalidCidrException, SubnetAddressesCapacityReachedException,
            FederatedNetworkNotFoundException, IOException {
        //set up
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setId(FEDERATED_COMPUTE_ID);
        FederatedComputeOrder federatedCompute = spy(new FederatedComputeOrder("", "", computeOrder));
        //exercise
        ComputeOrder computeReturned = orderController.addFederationUserDataIfApplied(federatedCompute, user);
        //verify
        assertEquals(computeOrder, computeReturned);
    }

    //test case: Tests if updates correctly the compute with the new id
    @Test
    public void testUpdateFederatedComputeId() {
        //set up
        String newId = "fake-compute-new-id";
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setId(FEDERATED_COMPUTE_ID);
        FederatedComputeOrder federatedCompute = spy(new FederatedComputeOrder(FEDERATED_COMPUTE_ID, FEDERATED_NETWORK_ID, computeOrder));

        DatabaseManager database = Mockito.mock(DatabaseManager.class);
        PowerMockito.mockStatic(DatabaseManager.class);
        Mockito.doNothing().when(database).putFederatedCompute(any(FederatedComputeOrder.class), any(FederationUser.class));
        BDDMockito.given(DatabaseManager.getInstance()).willReturn(database);

        assertEquals(FEDERATED_COMPUTE_ID, federatedCompute.getComputeOrder().getId());
        assertNotEquals(newId, federatedCompute.getComputeOrder().getId());
        //exercise
        orderController.updateIdOnComputeCreation(federatedCompute, newId);
        //verify
        assertNotEquals(FEDERATED_COMPUTE_ID, federatedCompute.getComputeOrder().getId());
        assertEquals(newId, federatedCompute.getComputeOrder().getId());
    }

    //test case: This test should add federated data to federated compute when receives a get method.
    @Test
    public void testGetFederatedCompute() throws UnauthenticatedUserException {
        //set up
        addNetworkIntoActiveMaps();
        addComputeIntoActiveMaps();
        ComputeInstance computeInstance = new ComputeInstance(FEDERATED_COMPUTE_ID, InstanceState.READY, "host", 2, 8, 20, "192.168.0.2");
        //exercise
        ComputeInstance federatedComputeInstance = orderController.addFederatedIpInGetInstanceIfApplied(computeInstance, user);
        //verify
        assertNotEquals(computeInstance, federatedComputeInstance);
        assertTrue(federatedComputeInstance instanceof FederatedComputeInstance);
        assertNotNull(((FederatedComputeInstance) federatedComputeInstance).getFederatedIp());
    }

    //test case: This test should throw an UnauthenticatedUserException since, a different user is trying to access it.
    @Test
    public void testGetFederatedComputeWithNonAuthenticatedUser() throws InvalidParameterException {
        //set up
        addNetworkIntoActiveMaps();
        addComputeIntoActiveMaps();
        ComputeInstance computeInstance = new ComputeInstance(FEDERATED_COMPUTE_ID);
        Map<String, String> fedUserAttrs = new HashMap<>();
        fedUserAttrs.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser nonAuthenticatedUser = new FederationUser("different-id", fedUserAttrs);
        //exercise
        try {
            orderController.addFederatedIpInGetInstanceIfApplied(computeInstance, nonAuthenticatedUser);
            fail();
        } catch (UnauthenticatedUserException e) {
            //verify
        }
    }

    //test case: This test should exactly the same computeInstance, since it's not a federated compute.
    @Test
    public void testGetNotFederatedCompute() throws UnauthenticatedUserException {
        //set up
        ComputeInstance computeInstance = new ComputeInstance(FEDERATED_COMPUTE_ID, InstanceState.READY, "host", 2, 8, 20, "192.168.0.2");
        //exercise
        ComputeInstance federatedComputeInstance = orderController.addFederatedIpInGetInstanceIfApplied(computeInstance, user);
        //verify
        assertEquals(computeInstance, federatedComputeInstance);
        assertFalse(federatedComputeInstance instanceof FederatedComputeInstance);
    }

    private void addComputeIntoActiveMaps() {
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setId(FEDERATED_COMPUTE_ID);
        computeOrder.setFederationUser(user);
        FederatedComputeOrder federatedCompute = spy(new FederatedComputeOrder(FEDERATED_NETWORK_ID, "", computeOrder));
        Map<String, FederatedComputeOrder> activeFederatedCompute = new ConcurrentHashMap<>();
        activeFederatedCompute.put(FEDERATED_COMPUTE_ID, federatedCompute);
        orderController.setActiveFederatedComputes(activeFederatedCompute);
    }

    private void addNetworkIntoActiveMaps() {
        String cidr = "10.10.10.0/24";
        Set<String> allowedMembers = new HashSet<>();
        Queue<String> freedIps = new LinkedList<>();
        List<String> computesIp = new ArrayList<>();
        FederatedNetworkOrder federatedNetwork = spy(new FederatedNetworkOrder(FEDERATED_NETWORK_ID, user, cidr, "test",
                allowedMembers, 1, freedIps, computesIp));
        Map<String, FederatedNetworkOrder> activeFederatedNetworks = new ConcurrentHashMap<>();
        activeFederatedNetworks.put(FEDERATED_NETWORK_ID, federatedNetwork);
        orderController.setActiveFederatedNetworks(activeFederatedNetworks);
    }

    private static ComputeOrder createComputeWithUserData(ComputeOrder computeOrder, UserData userData) {
        ComputeOrder newCompute = new ComputeOrder(computeOrder.getId(), computeOrder.getFederationUser(),
                computeOrder.getRequestingMember(), computeOrder.getProvidingMember(), computeOrder.getvCPU(),
                computeOrder.getMemory(), computeOrder.getDisk(), computeOrder.getImageId(),
                userData, computeOrder.getPublicKey(), computeOrder.getNetworksId());
        return newCompute;
    }
}
