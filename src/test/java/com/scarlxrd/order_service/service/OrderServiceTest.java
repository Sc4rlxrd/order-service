package com.scarlxrd.order_service.service;

import static org.junit.jupiter.api.Assertions.*;
import com.scarlxrd.order_service.config.rabbitmq.OrderPublisher;
import com.scarlxrd.order_service.dto.*;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.BusinessException;
import com.scarlxrd.order_service.mapper.OrderMapper;
import com.scarlxrd.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderMapper mapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private OrderPublisher orderPublisher;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderDTO dto;
    private Order order;
    private OrderResponseDTO responseDTO;
    private final String EMAIL = "cliente@teste.com";

    @BeforeEach
    void setUp() {

        OrderItemDTO item1 = new OrderItemDTO();
        item1.setBookId(UUID.randomUUID());
        item1.setQuantity(2);

        OrderItemDTO item2 = new OrderItemDTO();
        item2.setBookId(UUID.randomUUID());
        item2.setQuantity(1);

        dto = new CreateOrderDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setItems(List.of(item1, item2));

        // order que o mapper vai retornar
        order = new Order();
        order.setId(UUID.randomUUID());
        order.setClientId(dto.getClientId());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.CREATED);

        // response que o mapper vai retornar no final
        responseDTO = new OrderResponseDTO();
        responseDTO.setId(order.getId());
        responseDTO.setClientId(dto.getClientId());
        responseDTO.setTotalAmount(BigDecimal.ZERO);
        responseDTO.setStatus("PENDING");
    }

    @Test
    @DisplayName("Deve criar order e retornar OrderResponseDTO")
    void shouldCreateOrderAndReturnResponse() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class))).thenReturn(order);
        when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

        OrderResponseDTO result = orderService.create(dto, EMAIL);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(order.getId());
        assertThat(result.getClientId()).isEqualTo(dto.getClientId());
    }

    @Test
    @DisplayName("Deve salvar order com status PENDING e totalAmount zerado")
    void shouldSaveOrderWithPendingStatusAndZeroAmount() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class))).thenReturn(order);
        when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

        orderService.create(dto, EMAIL);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        Order saved = captor.getAllValues().getFirst();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve setar totalItems com o tamanho da lista de items do DTO")
    void shouldSetTotalItemsFromDtoItemsSize() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class))).thenReturn(order);
        when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

        orderService.create(dto, EMAIL);

        // segundo save é onde seta o totalItems
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(repository, times(2)).save(captor.capture());

        Order savedWithTotalItems = captor.getAllValues().get(1);
        assertThat(savedWithTotalItems.getTotalItems()).isEqualTo(dto.getItems().size());
    }


    @Test
    @DisplayName("Deve publicar OrderCreatedEvent com orderId e email corretos")
    void shouldPublishOrderCreatedEventWithCorrectData() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class))).thenReturn(order);
        when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

        orderService.create(dto, EMAIL);

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderPublisher).publishOrderCreated(captor.capture());

        OrderCreatedEvent published = captor.getValue();
        assertThat(published.getOrderId()).isEqualTo(order.getId());
        assertThat(published.getCustomerEmail()).isEqualTo(EMAIL);
    }


    @Test
    @DisplayName("Deve enviar BookValidationRequest para cada item do DTO")
    void shouldSendBookValidationRequestForEachItem() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class))).thenReturn(order);
        when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

        orderService.create(dto, EMAIL);


        verify(rabbitTemplate, times(dto.getItems().size()))
                .convertAndSend(
                        eq("book.events"),
                        eq("book.validate"),
                        any(BookValidationRequest.class)
                );
    }

    @Test
    @DisplayName("Deve enviar BookValidationRequest com dados corretos do item")
    void shouldSendBookValidationRequestWithCorrectData() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class))).thenReturn(order);
        when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

        orderService.create(dto, EMAIL);

        ArgumentCaptor<BookValidationRequest> captor =
                ArgumentCaptor.forClass(BookValidationRequest.class);

        verify(rabbitTemplate, times(2))
                .convertAndSend(eq("book.events"), eq("book.validate"), captor.capture());

        List<BookValidationRequest> requests = captor.getAllValues();

        // verifica que cada request tem o orderId certo e os dados do ‘item’
        assertThat(requests).allSatisfy(request ->
                assertThat(request.getOrderId()).isEqualTo(order.getId())
        );
        assertThat(requests.get(0).getBookId()).isEqualTo(dto.getItems().get(0).getBookId());
        assertThat(requests.get(0).getQuantity()).isEqualTo(dto.getItems().get(0).getQuantity());
        assertThat(requests.get(1).getBookId()).isEqualTo(dto.getItems().get(1).getBookId());
        assertThat(requests.get(1).getQuantity()).isEqualTo(dto.getItems().get(1).getQuantity());
    }

    @Test
    @DisplayName("Deve chamar repository.save exatamente 2 vezes")
    void shouldCallRepositorySaveTwice() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class))).thenReturn(order);
        when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

        orderService.create(dto, EMAIL);

        verify(repository, times(2)).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando mapper retornar null")
    void shouldThrowWhenMapperReturnsNull() {
        when(mapper.toEntity(dto)).thenReturn(null);

        assertThatThrownBy(() -> orderService.create(dto, EMAIL)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção quando repository.save falhar")
    void shouldThrowWhenRepositoryFails() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class)))
                .thenThrow(new RuntimeException("Banco indisponível"));

        assertThatThrownBy(() -> orderService.create(dto, EMAIL)).isInstanceOf(RuntimeException.class)
                .hasMessage("Banco indisponível");
    }

    @Test
    @DisplayName("Deve lançar exceção quando rabbitTemplate falhar ao enviar")
    void shouldThrowWhenRabbitTemplateFails() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class))).thenReturn(order);

        doThrow(new RuntimeException("RabbitMQ indisponível")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatThrownBy(() -> orderService.create(dto, EMAIL)).isInstanceOf(RuntimeException.class)
                .hasMessage("RabbitMQ indisponível");
    }

    @Test
    @DisplayName("Deve lançar exceção quando orderPublisher falhar")
    void shouldThrowWhenPublisherFails() {
        when(mapper.toEntity(dto)).thenReturn(order);
        when(repository.save(any(Order.class))).thenReturn(order);

        doThrow(new RuntimeException("Publisher indisponível")).when(orderPublisher)
                .publishOrderCreated(any(OrderCreatedEvent.class));

        assertThatThrownBy(() -> orderService.create(dto, EMAIL)).isInstanceOf(RuntimeException.class)
                .hasMessage("Publisher indisponível");
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando clientId for null")
    void shouldThrowWhenClientIdIsNull() {
        dto.setClientId(null);

        assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("ClientId is mandatory");

        verify(repository, never()).save(any());
        verify(orderPublisher, never()).publishOrderCreated(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), Optional.ofNullable(any()));
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando lista de items for null")
    void shouldThrowWhenItemsIsNull() {
        dto.setItems(null);

        assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The order must contain at least one item.");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando lista de items for vazia")
    void shouldThrowWhenItemsIsEmpty() {
        dto.setItems(List.of());

        assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The order must contain at least one item.");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando bookId de algum item for null")
    void shouldThrowWhenBookIdIsNull() {
        dto.getItems().getFirst().setBookId(null);

        assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("BookId is mandatory");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando quantity for zero")
    void shouldThrowWhenQuantityIsZero() {
        dto.getItems().getFirst().setQuantity(0);

        assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The quantity must be greater than zero.");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando quantity for negativo")
    void shouldThrowWhenQuantityIsNegative() {
        dto.getItems().getFirst().setQuantity(-1);

        assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The quantity must be greater than zero.");

        verify(repository, never()).save(any());
    }

}