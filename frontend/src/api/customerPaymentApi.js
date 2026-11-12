import { createCrudApi } from './crudApi';

const { list, get, create, remove } = createCrudApi('/customer-payments');

export const customerPaymentApi = { list, get, create, remove };
