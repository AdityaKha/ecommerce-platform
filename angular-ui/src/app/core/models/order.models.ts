export type OrderStatus = 'CREATED' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItemRequest {
  productId: number;
  quantity: number;
  unitPrice: number;
}

export interface OrderRequest {
  customerUsername: string;
  customerEmail: string;
  items: OrderItemRequest[];
}

export interface OrderResponse {
  id: number;
  customerUsername: string;
  customerEmail: string;
  status: OrderStatus;
  totalAmount: number;
  items: OrderItemRequest[];
  createdAt: string;
}
