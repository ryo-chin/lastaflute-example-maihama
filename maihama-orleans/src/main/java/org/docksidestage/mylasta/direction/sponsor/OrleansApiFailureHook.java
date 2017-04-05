package org.docksidestage.mylasta.direction.sponsor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.docksidestage.mylasta.direction.sponsor.OrleansApiFailureHook.OrleansUnifiedFailureResult.OrleansFailureErrorPart;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.web.api.ApiFailureHook;
import org.lastaflute.web.api.ApiFailureResource;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.validation.Required;

/**
 * @author jflute
 */
public class OrleansApiFailureHook implements ApiFailureHook {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final int BUSINESS_FAILURE_STATUS = HttpServletResponse.SC_BAD_REQUEST;

    // ===================================================================================
    //                                                                    Business Failure
    //                                                                    ================
    @Override
    public ApiResponse handleValidationError(ApiFailureResource resource) {
        final OrleansUnifiedFailureResult result = createFailureResult(OrleansUnifiedFailureType.VALIDATION_ERROR, resource);
        return asJson(result).httpStatus(BUSINESS_FAILURE_STATUS);
    }

    @Override
    public ApiResponse handleApplicationException(ApiFailureResource resource, RuntimeException cause) {
        final OrleansUnifiedFailureResult result = createFailureResult(OrleansUnifiedFailureType.BUSINESS_ERROR, resource);
        return asJson(result).httpStatus(BUSINESS_FAILURE_STATUS);
    }

    // ===================================================================================
    //                                                                      System Failure
    //                                                                      ==============
    @Override
    public OptionalThing<ApiResponse> handleClientException(ApiFailureResource resource, RuntimeException cause) {
        final OrleansUnifiedFailureResult result = createFailureResult(OrleansUnifiedFailureType.CLIENT_ERROR, resource);
        return OptionalThing.of(asJson(result)); // HTTP status will be automatically sent as client error for the cause
    }

    @Override
    public OptionalThing<ApiResponse> handleServerException(ApiFailureResource resource, Throwable cause) {
        return OptionalThing.empty(); // means empty body, HTTP status will be automatically sent as server error
    }

    // ===================================================================================
    //                                                                          JSON Logic
    //                                                                          ==========
    // -----------------------------------------------------
    //                                        Failure Result
    //                                        --------------
    protected OrleansUnifiedFailureResult createFailureResult(OrleansUnifiedFailureType failureType, ApiFailureResource resource) {
        return new OrleansUnifiedFailureResult(failureType, toErrors(resource));
    }

    protected List<OrleansFailureErrorPart> toErrors(ApiFailureResource resource) {
        return resource.getPropertyMessageMap().entrySet().stream().flatMap(entry -> {
            return toFailureErrorPart(resource, entry.getKey(), entry.getValue()).stream();
        }).collect(Collectors.toList());
    }

    protected List<OrleansFailureErrorPart> toFailureErrorPart(ApiFailureResource resource, String field, List<String> messageList) {
        final String delimiter = "|";
        return messageList.stream().map(message -> {
            if (message.contains(delimiter)) { // e.g. Length | min:{min}, max:{max}
                return createJsonistaError(resource, field, message, delimiter);
            } else { // e.g. Required
                return createSimpleError(field, message);
            }
        }).collect(Collectors.toList());
    }

    protected OrleansFailureErrorPart createJsonistaError(ApiFailureResource resource, String field, String message, String delimiter) {
        final String code = Srl.substringFirstFront(message, delimiter).trim(); // e.g. Length
        final String json = "{" + Srl.substringFirstRear(message, delimiter).trim() + "}"; // e.g. {min:{min}, max:{max}}
        final Map<String, Object> data = parseJsonistaData(resource, field, code, json);
        return new OrleansFailureErrorPart(field, code, data);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseJsonistaData(ApiFailureResource resource, String field, String code, String json) {
        try {
            final JsonManager jsonManager = resource.getRequestManager().getJsonManager();
            return jsonManager.fromJson(json, Map.class);
        } catch (RuntimeException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to parse client-managed message data.");
            br.addItem("Advice");
            br.addElement("Arrange your [app]_message.properties");
            br.addElement("for client-managed message way like this:");
            br.addElement("  constraints.Length.message = Length | min:{min}, max:{max}");
            br.addElement("  constraints.Required.message = Required");
            br.addElement("  ...");
            br.addItem("Message List");
            br.addElement(resource.getMessageList());
            br.addItem("Target Field");
            br.addElement(field);
            br.addItem("Error Code");
            br.addElement(code);
            br.addItem("Data as JSON");
            br.addElement(json);
            final String msg = br.buildExceptionMessage();
            throw new IllegalStateException(msg, e);
        }
    }

    protected OrleansFailureErrorPart createSimpleError(String field, String message) {
        return new OrleansFailureErrorPart(field, message, Collections.emptyMap());
    }

    // -----------------------------------------------------
    //                                         JSON Response
    //                                         -------------
    protected <RESULT> JsonResponse<RESULT> asJson(RESULT result) {
        return new JsonResponse<RESULT>(result);
    }

    // ===================================================================================
    //                                                                      Failure Result
    //                                                                      ==============
    public static class OrleansUnifiedFailureResult {

        @Required
        public final OrleansUnifiedFailureType cause;

        @NotNull
        @Valid
        public final List<OrleansFailureErrorPart> errors;

        public static class OrleansFailureErrorPart {

            @Required
            public final String field;

            @Required
            public final String code;

            @NotNull
            public final Map<String, Object> data;

            public OrleansFailureErrorPart(String field, String code, Map<String, Object> data) {
                this.field = field;
                this.code = code;
                this.data = data;
            }
        }

        public OrleansUnifiedFailureResult(OrleansUnifiedFailureType cause, List<OrleansFailureErrorPart> errors) {
            this.cause = cause;
            this.errors = errors;
        }
    }

    public static enum OrleansUnifiedFailureType {
        VALIDATION_ERROR, BUSINESS_ERROR, CLIENT_ERROR
        // SERVER_ERROR is implemented by 500.json
    }
}
