import { createCrudApi } from './crudApi';

const { list, get, create, remove } = createCrudApi('/supplier-payments');

export const supplierPaymentApi = { list, get, create, remove };
