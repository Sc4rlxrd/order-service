package com.scarlxrd.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scarlxrd.order_service.config.metrics.OutboxMetrics;
import com.scarlxrd.order_service.dto.*;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.BusinessException;
import com.scarlxrd.order_service.mapper.OrderMapper;
import com.scarlxrd.order_service.outbox.OutboxEvent;
import com.scarlxrd.order_service.outbox.OutboxRepository;
import com.scarlxrd.order_service.outbox.OutboxStatus;
import com.scarlxrd.order_service.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do OrderService")
class OrderServiceTest {

    @Mock private OrderRepository repository;
    @Mock private OrderMapper mapper;
    @Mock private OutboxRepository outboxRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private OutboxMetrics outboxMetrics;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderDTO dto;
    private Order order;
    private OrderResponseDTO responseDTO;

    private static final String EMAIL = "cliente@teste.com";
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final String USER_ID = CLIENT_ID.toString();

    @BeforeEach
    void setUp() {
        OrderItemDTO item1 = new OrderItemDTO();
        item1.setBookId(UUID.randomUUID());
        item1.setQuantity(2);

        OrderItemDTO item2 = new OrderItemDTO();
        item2.setBookId(UUID.randomUUID());
        item2.setQuantity(1);

        dto = new CreateOrderDTO();
        dto.setItems(List.of(item1, item2));

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setClientId(CLIENT_ID);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.CREATED);

        responseDTO = new OrderResponseDTO();
        responseDTO.setId(order.getId());
        responseDTO.setClientId(CLIENT_ID);
        responseDTO.setTotalAmount(BigDecimal.ZERO);
        responseDTO.setStatus("PENDING");
    }

    private void mockObjectMapperSuccessfully() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}");
    }

    @Nested
    @DisplayName("Cenários de sucesso")
    class SuccessTests {

        @Test
        @DisplayName("Deve criar pedido e retornar resposta")
        void shouldCreateOrderAndReturnResponse() throws JsonProcessingException {
            mockObjectMapperSuccessfully();

            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            OrderResponseDTO result = orderService.create(dto, EMAIL, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(order.getId());
            assertThat(result.getClientId()).isEqualTo(CLIENT_ID);
        }

        @Test
        @DisplayName("Deve salvar pedido com status PENDING e valor zero")
        void shouldSaveOrderWithPendingStatusAndZeroAmount() throws JsonProcessingException {
            mockObjectMapperSuccessfully();

            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            orderService.create(dto, EMAIL, USER_ID);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(repository).saveAndFlush(captor.capture());

            Order saved = captor.getValue();

            assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getClientId()).isEqualTo(CLIENT_ID);
        }

        @Test
        @DisplayName("Deve definir totalItems corretamente")
        void shouldSetTotalItemsFromDtoItemsSize() throws JsonProcessingException {
            mockObjectMapperSuccessfully();

            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            orderService.create(dto, EMAIL, USER_ID);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(repository).saveAndFlush(captor.capture());

            assertThat(captor.getValue().getTotalItems()).isEqualTo(dto.getItems().size());
        }

        @Test
        @DisplayName("Deve salvar evento de criação do pedido na outbox")
        void shouldSaveOrderCreatedOutboxEvent() throws JsonProcessingException {
            mockObjectMapperSuccessfully();

            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            orderService.create(dto, EMAIL, USER_ID);

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

            verify(outboxRepository, times(3)).save(captor.capture());

            OutboxEvent orderCreatedEvent = captor.getAllValues()
                    .stream()
                    .filter(event -> "ORDER_CREATED".equals(event.getEventType()))
                    .findFirst()
                    .orElseThrow();

            assertThat(orderCreatedEvent.getAggregateId()).isEqualTo(order.getId());
            assertThat(orderCreatedEvent.getAggregateType()).isEqualTo("ORDER");
            assertThat(orderCreatedEvent.getEventType()).isEqualTo("ORDER_CREATED");
            assertThat(orderCreatedEvent.getPayload()).isEqualTo("{}");
            assertThat(orderCreatedEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(orderCreatedEvent.getRetryCount()).isZero();
            assertThat(orderCreatedEvent.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Deve salvar validação de livros na outbox para cada item")
        void shouldSaveBookValidationOutboxEventForEachItem() throws JsonProcessingException {
            mockObjectMapperSuccessfully();

            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            orderService.create(dto, EMAIL, USER_ID);

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

            verify(outboxRepository, times(3)).save(captor.capture());

            List<OutboxEvent> bookValidationEvents = captor.getAllValues()
                    .stream()
                    .filter(event -> "BOOK_VALIDATE_REQUESTED".equals(event.getEventType()))
                    .toList();

            assertThat(bookValidationEvents).hasSize(dto.getItems().size());

            bookValidationEvents.forEach(event -> {
                assertThat(event.getAggregateId()).isEqualTo(order.getId());
                assertThat(event.getAggregateType()).isEqualTo("ORDER");
                assertThat(event.getEventType()).isEqualTo("BOOK_VALIDATE_REQUESTED");
                assertThat(event.getPayload()).isEqualTo("{}");
                assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
                assertThat(event.getRetryCount()).isZero();
                assertThat(event.getCreatedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("Deve salvar pedido exatamente uma vez")
        void shouldCallRepositorySaveOnce() throws JsonProcessingException {
            mockObjectMapperSuccessfully();

            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            orderService.create(dto, EMAIL, USER_ID);

            verify(repository, times(1)).saveAndFlush(any(Order.class));
        }
    }

    @Nested
    @DisplayName("Cenários de erro")
    class ErrorTests {

        @Test
        @DisplayName("Deve lançar erro quando repository falhar")
        void shouldThrowWhenRepositoryFails() {
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class)))
                    .thenThrow(new RuntimeException("Banco indisponível"));

            assertThatThrownBy(() -> orderService.create(dto, EMAIL, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Banco indisponível");
        }

        @Test
        @DisplayName("Deve lançar erro quando falhar ao criar evento de pedido na outbox")
        void shouldThrowWhenOrderCreatedOutboxEventFails() throws JsonProcessingException {
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);

            when(objectMapper.writeValueAsString(any(OrderCreatedEvent.class)))
                    .thenThrow(new JsonProcessingException("Erro ao serializar") {});

            assertThatThrownBy(() -> orderService.create(dto, EMAIL, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Failed to create order created outbox event");
        }

        @Test
        @DisplayName("Deve lançar erro quando falhar ao criar evento de validação de livros na outbox")
        void shouldThrowWhenBookValidationOutboxEventFails() throws JsonProcessingException {
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);

            when(objectMapper.writeValueAsString(any(OrderCreatedEvent.class)))
                    .thenReturn("{}");

            when(objectMapper.writeValueAsString(any(BookValidationRequest.class)))
                    .thenThrow(new JsonProcessingException("Erro ao serializar") {});

            assertThatThrownBy(() -> orderService.create(dto, EMAIL, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Failed to create book validation outbox event");
        }

        @Test
        @DisplayName("Deve lançar erro quando mapper retornar nulo")
        void shouldThrowWhenMapperReturnsNull() {
            when(mapper.toEntity(dto)).thenReturn(null);

            assertThatThrownBy(() -> orderService.create(dto, EMAIL, USER_ID))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Validações de negócio")
    class ValidationTests {

        @Test
        void shouldThrowWhenUserIdIsNull() {
            assertThatThrownBy(() -> orderService.create(dto, EMAIL, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Authenticated user not found");
        }

        @Test
        void shouldThrowWhenUserIdIsBlank() {
            assertThatThrownBy(() -> orderService.create(dto, EMAIL, " "))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Authenticated user not found");
        }

        @Test
        void shouldThrowWhenUserIdIsInvalid() {
            assertThatThrownBy(() -> orderService.create(dto, EMAIL, "invalid-user-id"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Invalid authenticated user");
        }

        @Test
        void shouldThrowWhenItemsIsNull() {
            dto.setItems(null);

            assertThatThrownBy(() -> orderService.create(dto, EMAIL, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("The order must contain at least one item.");
        }

        @Test
        void shouldThrowWhenItemsIsEmpty() {
            dto.setItems(List.of());

            assertThatThrownBy(() -> orderService.create(dto, EMAIL, USER_ID))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldThrowWhenBookIdIsNull() {
            dto.getItems().getFirst().setBookId(null);

            assertThatThrownBy(() -> orderService.create(dto, EMAIL, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("BookId is mandatory");
        }

        @Test
        void shouldThrowWhenQuantityIsZero() {
            dto.getItems().getFirst().setQuantity(0);

            assertThatThrownBy(() -> orderService.create(dto, EMAIL, USER_ID))
                    .isInstanceOf(BusinessException.class);
        }
    }
}