package org.motechproject.mots.errorhandling;

import java.util.Map;
import org.motechproject.mots.exception.BindingResultException;
import org.motechproject.mots.exception.EntityNotFoundException;
import org.motechproject.mots.exception.MotsAccessDeniedException;
import org.motechproject.mots.exception.MotsException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.supercsv.exception.SuperCsvException;

@ControllerAdvice
public class GlobalErrorHandling extends AbstractErrorHandling {

  @ExceptionHandler(MotsException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public ErrorResponse handleMotsException(MotsException ex) {
    return logAndGetErrorResponse(ex);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public ErrorResponse handleException(Exception ex) {
    String message = "Unexpected error occurred, check the log for more details";
    return logAndGetErrorResponse(message, ex);
  }

  @ExceptionHandler(BindingResultException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Map<String, String> handleBindingResultException(BindingResultException ex) {
    return ex.getErrors();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
    return new ErrorResponse(ex.getMessage());
  }

  @ExceptionHandler(SuperCsvException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public ErrorResponse handleSuperCsvException(SuperCsvException ex) {
    return logAndGetErrorResponse("The CSV file structure is invalid,"
        + " check if column number in every row is correct", ex);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public ErrorResponse entityNotFoundException(EntityNotFoundException ex) {
    return logAndGetErrorResponse(ex);
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ResponseBody
  public String handleAccessDeniedException(Exception ex, WebRequest request) {
    return ex.getMessage();
  }

  @ExceptionHandler(MotsAccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ResponseBody
  public String handleMotsAccessDeniedException(MotsAccessDeniedException ex) {
    return ex.getMessage();
  }
}
