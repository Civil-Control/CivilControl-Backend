package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Pago por cheque. La cuenta bancaria es obligatoria; la chequera es opcional, " +
        "pero si se especifica el número de cheque debe estar dentro del rango y no haber sido usado antes.")
public record CheckPaymentDTO(

        @NotNull(message = "{payment.details.required}", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @NotNull(message = "{payment.dueDate.required}", groups = OnCreate.class)
        LocalDate dueDate,

        @Size(max = 50, message = "{payment.checkNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        String checkNumber,

        @Schema(description = "Cuenta bancaria emisora del cheque. Reemplaza al antiguo `bankName`.")
        @NotNull(message = "{treasury.bankAccount.required}", groups = OnCreate.class)
        Long bankAccountId,

        @Schema(description = "Chequera (talonario). Opcional; si se especifica, el número de cheque " +
                "debe pertenecer al rango configurado y la cuenta bancaria se toma desde la chequera.")
        Long checkbookId,

        boolean deleted
) {}
