package com.example.application.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.application.data.model.Order;
import com.example.application.data.repository.OrdersRepository;

import java.util.List;

public class OrdersViewModel extends ViewModel {

    private OrdersRepository orderRepository;
    private MutableLiveData<List<Order>> ordersLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public OrdersViewModel(OrdersRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void fetchOrders(String token) {
        orderRepository.fetchOrders(token, new OrdersRepository.OrderCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                ordersLiveData.postValue(orders);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
            }
        });
    }

    public void fetchAdminOrders(String token) {
        orderRepository.fetchAdminOrders(token, new OrdersRepository.OrderCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                ordersLiveData.postValue(orders);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
            }
        });
    }

    public void deleteOrder(String token, int orderId) {
        orderRepository.deleteOrder(token, orderId, new OrdersRepository.OrderDeleteCallback() {
            @Override
            public void onSuccess() {
                fetchOrders(token);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
            }
        });
    }

    public void updateOrderStatus(String token, int orderId, String newStatus) {
        orderRepository.updateOrderStatus(token, orderId, newStatus, new OrdersRepository.OrderUpdateCallback() {
            @Override
            public void onSuccess() {
                fetchAdminOrders(token);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
            }
        });
    }

    public LiveData<List<Order>> getOrdersLiveData() {
        return ordersLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }
}

