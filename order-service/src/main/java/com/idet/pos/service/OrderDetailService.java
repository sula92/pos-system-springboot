package com.idet.pos.service;

import com.idet.pos.dto.OrderDetailDTO;
import com.idet.pos.entity.OrderDetail;
import com.idet.pos.repository.OrderDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderDetailService {

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Transactional(readOnly = true)
    public List<OrderDetailDTO> findByOrderId(String orderId) {
        return toDTOList(orderDetailRepository.findByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderDetailDTO> findByOrderIdWithOrderGraph(String orderId) {
        return toDTOList(orderDetailRepository.findByOrderIdWithOrderGraph(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderDetailDTO> findByOrderIdJoinFetchOrder(String orderId) {
        return toDTOList(orderDetailRepository.findByOrderIdJoinFetchOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderDetailDTO> findByOrderIdNative(String orderId) {
        return toDTOList(orderDetailRepository.findByOrderIdNative(orderId));
    }

    @Transactional(readOnly = true)
    public int sumOrderedQtyByItemCodeNative(String itemCode) {
        return orderDetailRepository.sumOrderedQtyByItemCodeNative(itemCode);
    }


    private List<OrderDetailDTO> toDTOList(List<OrderDetail> details) {
        List<OrderDetailDTO> dtos = new ArrayList<>();
        for (OrderDetail detail : details) {
            dtos.add(new OrderDetailDTO(detail.getOrderId(), detail.getItemCode(), detail.getQty(), detail.getUnitPrice()));
        }
        return dtos;
    }
}

