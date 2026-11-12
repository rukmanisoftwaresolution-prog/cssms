import { toNumber, round2 } from './formatters';

/** Purchase line: amount = qty * rate, gst = amount * gst% */
export function purchaseLine(item) {
  const amount = toNumber(item?.quantity) * toNumber(item?.purchaseRatePerTon);
  const gstAmount = (amount * toNumber(item?.gstPercentage)) / 100;
  return { amount: round2(amount), gstAmount: round2(gstAmount), totalAmount: round2(amount + gstAmount) };
}

/** Sales line: amount = qty * rate - discount, gst = amount * gst% */
export function salesLine(item) {
  const gross = toNumber(item?.quantity) * toNumber(item?.rate);
  const discount = toNumber(item?.discount);
  const amount = gross - discount;
  const gstAmount = (amount * toNumber(item?.gstPercentage)) / 100;
  return {
    gross: round2(gross),
    discount: round2(discount),
    amount: round2(amount),
    gstAmount: round2(gstAmount),
    totalAmount: round2(amount + gstAmount),
  };
}

export function sumBy(list, key) {
  return round2(list.reduce((s, x) => s + toNumber(x[key]), 0));
}
