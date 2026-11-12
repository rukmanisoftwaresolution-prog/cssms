import { salesOrderApi } from '../../api/salesOrderApi';
import { saveResponse } from '../../utils/download';
import { getErrorMessage } from '../../utils/errorMessage';

export async function downloadInvoice(order, notify) {
  try {
    const res = await salesOrderApi.invoice(order.id);
    saveResponse(res, `invoice-${order.invoiceNumber || order.id}.pdf`);
  } catch (err) {
    notify.error(getErrorMessage(err, 'Failed to download invoice'));
  }
}
