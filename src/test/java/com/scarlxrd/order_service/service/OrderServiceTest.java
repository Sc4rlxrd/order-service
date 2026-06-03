package com.scarlxrd.order_service.service;

import com.scarlxrd.order_service.config.rabbitmq.OrderPublisher;
import com.scarlxrd.order_service.dto.*;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.BusinessException;
import com.scarlxrd.order_service.mapper.OrderMapper;
import com.scarlxrd.order_service.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private OrderPublisher orderPublisher;

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
        TransactionSynchronizationManager.initSynchronization();

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

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void executeAfterCommit() {
        TransactionSynchronizationManager
                .getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
    }

    @Nested
    @DisplayName("Cenários de sucesso")
    class SuccessTests {

        @Test
        @DisplayName("Deve criar pedido e retornar resposta")
        void shouldCreateOrderAndReturnResponse() {
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
        void shouldSaveOrderWithPendingStatusAndZeroAmount() {
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
        void shouldSetTotalItemsFromDtoItemsSize() {
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            orderService.create(dto, EMAIL, USER_ID);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(repository).saveAndFlush(captor.capture());

            assertThat(captor.getValue().getTotalItems()).isEqualTo(dto.getItems().size());
        }

        @Test
        @DisplayName("Deve publicar evento de criação do pedido")
        void shouldPublishOrderCreatedEvent() {
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            orderService.create(dto, EMAIL, USER_ID);
            executeAfterCommit();

            ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);

            verify(orderPublisher).publishOrderCreated(captor.capture());

            assertThat(captor.getValue().getOrderId()).isEqualTo(order.getId());
            assertThat(captor.getValue().getCustomerEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("Deve enviar validação de livros para cada item")
        void shouldSendBookValidationForEachItem() {
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            orderService.create(dto, EMAIL, USER_ID);
            executeAfterCommit();

            verify(rabbitTemplate, times(dto.getItems().size()))
                    .convertAndSend(
                            eq("book.events"),
                            eq("book.validate"),
                            any(BookValidationRequest.class)
                    );
        }

        @Test
        @DisplayName("Deve salvar pedido exatamente uma vez")
        void shouldCallRepositorySaveOnce() {
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
        @DisplayName("Deve lançar erro quando RabbitMQ falhar")
        void shouldThrowWhenRabbitFails() {
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any())).thenReturn(responseDTO);

            doThrow(new RuntimeException("RabbitMQ indisponível"))
                    .when(rabbitTemplate)
                    .convertAndSend(
                            anyString(),
                            anyString(),
                            any(BookValidationRequest.class)
                    );

            assertThatThrownBy(() -> {
                orderService.create(dto, EMAIL, USER_ID);
                executeAfterCommit();
            })
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("RabbitMQ indisponível");
        }

        @Test
        @DisplayName("Deve lançar erro quando publisher falhar")
        void shouldThrowWhenPublisherFails() {
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any())).thenReturn(responseDTO);

            doThrow(new RuntimeException("Publisher indisponível"))
                    .when(orderPublisher)
                    .publishOrderCreated(any());

            assertThatThrownBy(() -> {
                orderService.create(dto, EMAIL, USER_ID);
                executeAfterCommit();
            })
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Publisher indisponível");
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