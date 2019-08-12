package cloud.fogbow.fns.core.datastore.orderstorage;

import cloud.fogbow.common.datastore.FogbowDatabaseService;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.fns.api.http.response.AssignedIp;
import cloud.fogbow.fns.constants.Messages;
import cloud.fogbow.fns.core.ComputeIdToFederatedNetworkIdMapping;
import cloud.fogbow.fns.core.exceptions.InvalidCidrException;
import cloud.fogbow.fns.core.exceptions.SubnetAddressesCapacityReachedException;
import cloud.fogbow.fns.core.model.FederatedNetworkOrder;
import cloud.fogbow.fns.core.model.OrderState;
import cloud.fogbow.fns.utils.FederatedNetworkUtil;
import org.apache.log4j.Logger;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecoveryService extends FogbowDatabaseService<FederatedNetworkOrder> {

    @Autowired
    private OrderRepository orderRepository;

    private static final Logger LOGGER = Logger.getLogger(RecoveryService.class);

    public RecoveryService() {}

    public void put(FederatedNetworkOrder order) throws UnexpectedException {
        order.serializeSystemUser();
        safeSave(order, this.orderRepository);
    }

    public List<FederatedNetworkOrder> readActiveOrdersByState(OrderState orderState) {
        List<FederatedNetworkOrder> orders = orderRepository.findByOrderState(orderState);
        for (FederatedNetworkOrder order: orders) {
            if (!(order.getOrderState().equals(OrderState.DEACTIVATED))) {
                try {
                    ComputeIdToFederatedNetworkIdMapping mapper = ComputeIdToFederatedNetworkIdMapping.getInstance();
                    for(AssignedIp ip : order.getAssignedIps()) {
                        mapper.put(ip.getComputeId(), order.getId());
                    }
                    order.fillCacheOfFreeIps();
                } catch (SubnetAddressesCapacityReachedException e) {
                    LOGGER.info(Messages.Exception.NO_MORE_IPS_AVAILABLE);
                } catch (InvalidCidrException e) {
                    LOGGER.error(Messages.Error.INVALID_CIDR);
                }
            }
        }
        return orders;
    }

    protected void setOrderRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
