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
import org.junit.jupiter.api.Nested;
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
@DisplayName("Testes do OrderService")
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

    @Nested
    @DisplayName("Cenários de sucesso")
    class SuccessTests {

        @Test
        @DisplayName("Deve criar pedido e retornar resposta")
        void shouldCreateOrderAndReturnResponse() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            // When
            OrderResponseDTO result = orderService.create(dto, EMAIL);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(order.getId());
            assertThat(result.getClientId()).isEqualTo(dto.getClientId());
        }

        @Test
        @DisplayName("Deve salvar pedido com status PENDING e valor zero")
        void shouldSaveOrderWithPendingStatusAndZeroAmount() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            // When
            orderService.create(dto, EMAIL);

            // Then
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(repository, atLeastOnce()).save(captor.capture());

            Order saved = captor.getAllValues().getFirst();
            assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Deve definir totalItems corretamente")
        void shouldSetTotalItemsFromDtoItemsSize() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            // When
            orderService.create(dto, EMAIL);

            // Then
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(repository, times(2)).save(captor.capture());

            Order saved = captor.getAllValues().get(1);
            assertThat(saved.getTotalItems()).isEqualTo(dto.getItems().size());
        }

        @Test
        @DisplayName("Deve publicar evento de criação do pedido")
        void shouldPublishOrderCreatedEvent() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            // When
            orderService.create(dto, EMAIL);

            // Then
            ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(orderPublisher).publishOrderCreated(captor.capture());

            OrderCreatedEvent event = captor.getValue();
            assertThat(event.getOrderId()).isEqualTo(order.getId());
            assertThat(event.getCustomerEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("Deve enviar validação de livros para cada item")
        void shouldSendBookValidationForEachItem() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            // When
            orderService.create(dto, EMAIL);

            // Then
            verify(rabbitTemplate, times(dto.getItems().size()))
                    .convertAndSend(
                            eq("book.events"),
                            eq("book.validate"),
                            any(BookValidationRequest.class));
        }

        @Test
        @DisplayName("Deve enviar validação de livros com dados corretos")
        void shouldSendBookValidationWithCorrectData() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            // When
            orderService.create(dto, EMAIL);

            // Then
            ArgumentCaptor<BookValidationRequest> captor = ArgumentCaptor.forClass(BookValidationRequest.class);

            verify(rabbitTemplate, times(dto.getItems().size())).convertAndSend(
                    eq("book.events"),
                    eq("book.validate"),
                    captor.capture());

            List<BookValidationRequest> requests = captor.getAllValues();

            assertThat(requests).allSatisfy(req -> assertThat(req.getOrderId()).isEqualTo(order.getId()));

            assertThat(requests.get(0).getBookId()).isEqualTo(dto.getItems().get(0).getBookId());
            assertThat(requests.get(0).getQuantity()).isEqualTo(dto.getItems().get(0).getQuantity());
            assertThat(requests.get(1).getBookId()).isEqualTo(dto.getItems().get(1).getBookId());
            assertThat(requests.get(1).getQuantity()).isEqualTo(dto.getItems().get(1).getQuantity());
        }

        @Test
        @DisplayName("Deve salvar pedido exatamente duas vezes")
        void shouldCallRepositorySaveTwice() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class))).thenReturn(order);
            when(mapper.toResponse(any(Order.class))).thenReturn(responseDTO);

            // When
            orderService.create(dto, EMAIL);

            // Then
            verify(repository, times(2)).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("Cenários de erro de infraestrutura")
    class ErrorTests {

        @Test
        @DisplayName("Deve lançar erro quando repository falhar")
        void shouldThrowWhenRepositoryFails() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class)))
                    .thenThrow(new RuntimeException("Banco indisponível"));

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Banco indisponível");
        }

        @Test
        @DisplayName("Deve lançar erro quando RabbitMQ falhar")
        void shouldThrowWhenRabbitFails() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class))).thenReturn(order);

            doThrow(new RuntimeException("RabbitMQ indisponível"))
                    .when(rabbitTemplate)
                    .convertAndSend(anyString(), anyString(), Optional.ofNullable(any()));

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("RabbitMQ indisponível");
        }

        @Test
        @DisplayName("Deve lançar erro quando publisher falhar")
        void shouldThrowWhenPublisherFails() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(order);
            when(repository.save(any(Order.class))).thenReturn(order);

            doThrow(new RuntimeException("Publisher indisponível"))
                    .when(orderPublisher)
                    .publishOrderCreated(any());

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Publisher indisponível");
        }

        @Test
        @DisplayName("Deve lançar erro quando mapper retornar nulo")
        void shouldThrowWhenMapperReturnsNull() {
            // Given
            when(mapper.toEntity(dto)).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Validações de negócio")
    class ValidationTests {

        @Test
        @DisplayName("Deve lançar exceção quando clientId for nulo")
        void shouldThrowWhenClientIdIsNull() {
            // Given
            dto.setClientId(null);

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("ClientId is mandatory");
        }

        @Test
        @DisplayName("Deve lançar exceção quando lista de itens for nula")
        void shouldThrowWhenItemsIsNull() {
            // Given
            dto.setItems(null);

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("The order must contain at least one item.");
        }

        @Test
        @DisplayName("Deve lançar exceção quando lista de itens estiver vazia")
        void shouldThrowWhenItemsIsEmpty() {
            // Given
            dto.setItems(List.of());

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("The order must contain at least one item.");
        }

        @Test
        @DisplayName("Deve lançar exceção quando bookId for nulo")
        void shouldThrowWhenBookIdIsNull() {
            // Given
            dto.getItems().getFirst().setBookId(null);

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("BookId is mandatory");
        }

        @Test
        @DisplayName("Deve lançar exceção quando quantidade for zero")
        void shouldThrowWhenQuantityIsZero() {
            // Given
            dto.getItems().getFirst().setQuantity(0);

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("The quantity must be greater than zero.");
        }

        @Test
        @DisplayName("Deve lançar exceção quando quantidade for negativa")
        void shouldThrowWhenQuantityIsNegative() {
            // Given
            dto.getItems().getFirst().setQuantity(-1);

            // When / Then
            assertThatThrownBy(() -> orderService.create(dto, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("The quantity must be greater than zero.");
        }
    }
}