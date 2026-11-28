package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.model.exception.UserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.CapacityConfigurationMissingException;
import com.campusdual.cd2026mnd1g1.model.exception.CheckRecordConflictException;
import com.campusdual.cd2026mnd1g1.model.exception.CapacityExceededException;
import com.campusdual.cd2026mnd1g1.model.exception.DuplicateReservationException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.TutorAccountInconsistentException;
import com.campusdual.cd2026mnd1g1.model.exception.TutorAccountNotLinkedException;
import com.campusdual.cd2026mnd1g1.model.exception.DuplicateTutorDogSubmissionException;
import com.campusdual.cd2026mnd1g1.model.exception.GroomingRequestTransitionConflictException;
import com.campusdual.cd2026mnd1g1.model.exception.GroomingAppointmentAssignmentConflictException;
import com.campusdual.cd2026mnd1g1.model.exception.GroomingAppointmentManagementConflictException;
import com.campusdual.cd2026mnd1g1.model.exception.ReservationDateNotAllowedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);
    private static final ZoneId ERROR_TIMESTAMP_ZONE = ZoneOffset.UTC;

    /**
     * Mapea errores de negocio de recursos no encontrados.
     *
     * @param exception excepcion de negocio
     * @return respuesta HTTP 404
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * Mapea recursos de dominio no encontrados.
     *
     * @param exception excepcion de recurso inexistente
     * @return respuesta HTTP 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * Mapea errores de permisos insuficientes.
     *
     * @param exception excepcion de acceso denegado
     * @return respuesta HTTP 403
     */
    @ExceptionHandler({AccessDeniedException.class, OperationalUserNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(RuntimeException exception) {
        String message = exception instanceof OperationalUserNotFoundException
                ? exception.getMessage()
                : "No tienes permisos para realizar esta operacion";
        return buildErrorResponse(HttpStatus.FORBIDDEN, message);
    }

    /**
     * Distingue una cuenta de tutor aún no vinculada de una lista vacía.
     *
     * @param exception cuenta sin relación explícita
     * @return respuesta de configuración requerida
     */
    @ExceptionHandler(TutorAccountNotLinkedException.class)
    public ResponseEntity<Map<String, Object>> handleTutorAccountNotLinked(
            TutorAccountNotLinkedException exception
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "TUTOR_ACCOUNT_NOT_LINKED",
                exception.getMessage()
        );
    }

    /**
     * Informa de una relación interna incoherente sin exponer IDs ni datos personales.
     *
     * @param exception configuración incoherente
     * @return respuesta de incidencia de configuración
     */
    @ExceptionHandler(TutorAccountInconsistentException.class)
    public ResponseEntity<Map<String, Object>> handleTutorAccountInconsistent(
            TutorAccountInconsistentException exception
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "TUTOR_ACCOUNT_INCONSISTENT",
                exception.getMessage()
        );
    }

    /**
     * Mapea errores de validacion de parametros o payload.
     *
     * @param exception excepcion de validacion
     * @return respuesta HTTP 400
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception exception) {
        String message = exception.getMessage();
        if (exception instanceof HttpMessageNotReadableException) {
            message = "El cuerpo de la petición no tiene un formato válido";
        } else if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            FieldError fieldError = methodArgumentNotValidException.getBindingResult().getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                message = fieldError.getDefaultMessage();
            }
        } else if (exception instanceof MethodArgumentTypeMismatchException mismatchException) {
            message = "El valor del parámetro " + mismatchException.getName() + " no tiene un formato válido";
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Informa cuando la carga supera los límites configurados del servidor.
     *
     * @param exception límite excedido
     * @return respuesta HTTP 413
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "La foto no puede superar 5 MB");
    }

    /**
     * Registra el fallo técnico de almacenamiento sin devolver rutas ni trazas.
     *
     * @param exception error interno de almacenamiento
     * @return respuesta HTTP 500
     */
    @ExceptionHandler(TutorDogPhotoStorageException.class)
    public ResponseEntity<Map<String, Object>> handlePhotoStorageFailure(
            TutorDogPhotoStorageException exception
    ) {
        LOGGER.error("Fallo al almacenar o limpiar una foto de perro", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo procesar la foto del perro");
    }

    /**
     * Mapea conflictos por reglas de negocio.
     *
     * @param exception excepcion de estado incompatible
     * @return respuesta HTTP 409
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    /**
     * Mapea el uso de una clave idempotente con datos diferentes.
     *
     * @param exception conflicto de clave de envío
     * @return respuesta HTTP 409
     */
    @ExceptionHandler(DuplicateTutorDogSubmissionException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateTutorDogSubmission(
            DuplicateTutorDogSubmissionException exception
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", exception.getMessage());
    }

    /**
     * Mapea conflictos al registrar presencia sin exponer detalles de persistencia.
     *
     * @param exception conflicto funcional de registro de entrada o salida
     * @return respuesta HTTP 409
     */
    @ExceptionHandler(CheckRecordConflictException.class)
    public ResponseEntity<Map<String, Object>> handleCheckRecordConflict(
            CheckRecordConflictException exception
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    /**
     * Mapea decisiones repetidas o no permitidas sobre solicitudes de peluquería.
     *
     * @param exception conflicto de transición
     * @return respuesta HTTP 409
     */
    @ExceptionHandler(GroomingRequestTransitionConflictException.class)
    public ResponseEntity<Map<String, Object>> handleGroomingRequestTransitionConflict(
            GroomingRequestTransitionConflictException exception
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "GROOMING_REQUEST_NOT_PENDING",
                exception.getMessage()
        );
    }

    /**
     * Mapea intentos de asignar citas que ya no están pendientes.
     *
     * @param exception conflicto de transición de cita
     * @return respuesta HTTP 409
     */
    @ExceptionHandler(GroomingAppointmentAssignmentConflictException.class)
    public ResponseEntity<Map<String, Object>> handleGroomingAppointmentAssignmentConflict(
            GroomingAppointmentAssignmentConflictException exception
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "GROOMING_APPOINTMENT_NOT_PENDING",
                exception.getMessage()
        );
    }

    /**
     * Mapea intentos de modificar citas completadas o canceladas.
     *
     * @param exception conflicto de estado de cita
     * @return respuesta HTTP 409
     */
    @ExceptionHandler(GroomingAppointmentManagementConflictException.class)
    public ResponseEntity<Map<String, Object>> handleGroomingAppointmentManagementConflict(
            GroomingAppointmentManagementConflictException exception
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "GROOMING_APPOINTMENT_NOT_MODIFIABLE",
                exception.getMessage()
        );
    }

    /**
     * Convierte la colisión de unicidad de entradas/salidas en un conflicto funcional.
     *
     * @param exception error de integridad de persistencia
     * @return respuesta de conflicto para la restricción de presencia o error genérico
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        String causeMessage = exception.getMostSpecificCause().getMessage();
        if (causeMessage != null
                && causeMessage.toLowerCase(Locale.ROOT).contains("uq_check_registro_reserva_tipo")) {
            return buildErrorResponse(
                    HttpStatus.CONFLICT,
                    "CHECK_RECORD_ALREADY_RECORDED",
                    "Ya existe un registro de entrada o salida para esta reserva."
            );
        }
        return handleGenericException(exception);
    }

    /**
     * Mapea conflictos específicos de reserva para que el cliente pueda actuar.
     *
     * @param exception conflicto de reserva
     * @return respuesta HTTP 409 con código funcional
     */
    @ExceptionHandler(DuplicateReservationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateReservation(
            DuplicateReservationException exception
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "DUPLICATE_RESERVATION", exception.getMessage());
    }

    /**
     * Mapea el agotamiento de una franja concreta.
     *
     * @param exception error de aforo
     * @return respuesta HTTP 409 con código funcional
     */
    @ExceptionHandler(CapacityExceededException.class)
    public ResponseEntity<Map<String, Object>> handleCapacityExceeded(CapacityExceededException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, "CAPACITY_FULL", exception.getMessage());
    }

    /**
     * Mapea la falta de grupos configurados para el centro.
     *
     * @param exception configuración ausente
     * @return respuesta HTTP 409 con código funcional
     */
    @ExceptionHandler(CapacityConfigurationMissingException.class)
    public ResponseEntity<Map<String, Object>> handleCapacityConfigurationMissing(
            CapacityConfigurationMissingException exception
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "CAPACITY_NOT_CONFIGURED", exception.getMessage());
    }

    /**
     * Mapea las fechas que incumplen el calendario operativo aprobado.
     *
     * @param exception fecha no permitida
     * @return respuesta HTTP 422 con código funcional
     */
    @ExceptionHandler(ReservationDateNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleReservationDateNotAllowed(
            ReservationDateNotAllowedException exception
    ) {
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "DATE_NOT_ALLOWED",
                exception.getMessage()
        );
    }

    /**
     * Mapea errores inesperados no controlados.
     *
     * @param exception excepcion no controlada
     * @return respuesta HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception exception) {
        LOGGER.error("Error REST no controlado: {}", exception.getClass().getName(), exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno no controlado");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        return buildErrorResponse(status, null, message);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String code,
            String message
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now(ERROR_TIMESTAMP_ZONE).toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        if (code != null) {
            body.put("code", code);
        }
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
