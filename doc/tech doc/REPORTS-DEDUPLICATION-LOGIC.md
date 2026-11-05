# Reports Deduplication Logic

## Problem

In the Money Outflows Report, certain entities (SalaryPayment, FuelLoad, Repair, StockPurchase) can be **linked** to a TransactionalDocument (invoice). When an entity is linked, its amount is included in the invoice's total via `DocumentTotalRecalculator`:

```
Invoice Total = SUM(ItemDetails) + SUM(linked FuelLoads + Repairs + SalaryPayments + StockPurchases) + Taxes
```

If both the invoice and the linked entity appear in the same report, the linked entity's amount gets counted **twice**:
- Once as part of the invoice total
- Once as an individual line item

**Example**: A salary of $1,000,000 linked to an invoice of $1,210,000 (salary + 21% IVA) would show as $2,210,000 total instead of $1,210,000.

## Solution

### Backend

#### 1. `linkedDocumentId` field on `ReportItemDTO`

Each report item carries a `linkedDocumentId` (nullable `Long`):
- **Entities that can link**: SalaryPayment, FuelLoad, Repair, StockPurchase → set to the TransactionalDocument's ID when linked
- **Entities that cannot link**: ServicePayment, LicencePlatePayment, PolicyPayment → always `null`
- **Invoices themselves**: always `null` (they are the parent, not the linked child)

#### 2. `duplicatedAmount` field on report DTOs

Both `MoneyOutflowReportDTO` and `MoneyOutflowReportPreviewDTO` include a `duplicatedAmount` (`BigDecimal`).

#### 3. `calculateDuplicatedAmount()` in `ReportService`

```java
private BigDecimal calculateDuplicatedAmount(List<ReportItemDTO> items) {
    // 1. Collect all INVOICE item IDs present in the report
    Set<Long> invoiceIds = items.stream()
        .filter(item -> item.getCategory() == MoneyOutflowCategory.INVOICE)
        .map(ReportItemDTO::getId)
        .collect(Collectors.toSet());

    // 2. Sum amounts of items where linkedDocumentId matches an invoice in the report
    return items.stream()
        .filter(item -> item.getLinkedDocumentId() != null)
        .filter(item -> invoiceIds.contains(item.getLinkedDocumentId()))
        .map(ReportItemDTO::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

**Key insight**: Only items whose `linkedDocumentId` matches an invoice ID **that is also present in the report** are counted as duplicated. If the invoice is excluded by filters (e.g., filtered by category), there's no duplication.

#### 4. Entity linking mechanisms

| Entity | Link field | Link type |
|--------|-----------|-----------|
| SalaryPayment | `transactionalDocument` | `@ManyToOne` (JPA relation) |
| FuelLoad | `transactionalDocument` | `@ManyToOne` (JPA relation) |
| Repair | `transactionalDocument` | `@ManyToOne` (JPA relation) |
| StockPurchase | `transactionalDocumentId` | `Long` (manual FK) |
| ServicePayment | — | Cannot link |
| LicencePlatePayment | — | Cannot link |
| PolicyPayment | — | Cannot link |

### Frontend

#### 1. Visual indicators

- **Dedup info bar**: Yellow banner below KPIs showing duplicated amount and adjusted total
- **Linked row highlight**: Table rows with `linkedDocumentId` get a yellow background
- **"vinculado" badge**: Small badge next to description text for linked items

#### 2. Adjusted total calculation

```typescript
getAdjustedTotal(): number {
    return reportData.totalAmount - (reportData.duplicatedAmount ?? 0);
}
```

### PDF/Excel Exporters

Both exporters show:
- **TOTAL GENERAL**: Raw sum of all items
- **MONTO DUPLICADO**: Amount that is double-counted (shown only when > 0)
- **TOTAL AJUSTADO**: `totalAmount - duplicatedAmount` (shown only when duplication exists)

## New Category: STOCK_PURCHASE

Added `STOCK_PURCHASE` ("Compra de Stock") as the 8th money outflow category.

- **Entity**: `StockPurchase` (fields: date, stockId, quantity, unitPrice, totalAmount, notes, transactionalDocumentId)
- **No projectArea**: Like insurance, stock purchases are excluded when `projectAreaIds` filter is active
- **Stock name**: Resolved via `StockRepository.findById()` for the description

## Additional Fix: ServicePayment paymentMethod

The `collectFromServicePayments()` method previously set `paymentMethod(null)`. Now correctly maps `sp.getPaymentMethod().getDisplayName()`.
