/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Data Lake Analytics job error details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InnerError {
    /**
     * The specific identifier for the type of error encountered in the activity.
     */
    @JsonProperty(value = "errorId", access = JsonProperty.Access.WRITE_ONLY)
    private String errorId;

    /**
     * The severity level of the failure. Possible values include: 'Warning', 'Error', 'Info', 'SevereWarning',
     * 'Deprecated', 'UserWarning'.
     */
    @JsonProperty(value = "severity", access = JsonProperty.Access.WRITE_ONLY)
    private SeverityTypes severity;

    /**
     * The ultimate source of the failure (usually either SYSTEM or USER).
     */
    @JsonProperty(value = "source", access = JsonProperty.Access.WRITE_ONLY)
    private String source;

    /**
     * The user friendly error message for the failure.
     */
    @JsonProperty(value = "message", access = JsonProperty.Access.WRITE_ONLY)
    private String message;

    /**
     * The error message description.
     */
    @JsonProperty(value = "description", access = JsonProperty.Access.WRITE_ONLY)
    private String description;

    /**
     * The details of the error message.
     */
    @JsonProperty(value = "details", access = JsonProperty.Access.WRITE_ONLY)
    private String details;

    /**
     * The diagnostic error code.
     */
    @JsonProperty(value = "diagnosticCode", access = JsonProperty.Access.WRITE_ONLY)
    private Integer diagnosticCode;

    /**
     * The component that failed.
     */
    @JsonProperty(value = "component", access = JsonProperty.Access.WRITE_ONLY)
    private String component;

    /**
     * The recommended resolution for the failure, if any.
     */
    @JsonProperty(value = "resolution", access = JsonProperty.Access.WRITE_ONLY)
    private String resolution;

    /**
     * The link to MSDN or Azure help for this type of error, if any.
     */
    @JsonProperty(value = "helpLink", access = JsonProperty.Access.WRITE_ONLY)
    private String helpLink;

    /**
     * The internal diagnostic stack trace if the user requesting the job error details has sufficient permissions it
     * will be retrieved, otherwise it will be empty.
     */
    @JsonProperty(value = "internalDiagnostics", access = JsonProperty.Access.WRITE_ONLY)
    private String internalDiagnostics;

    /**
     * The inner error of this specific job error message, if any.
     */
    @JsonProperty(value = "innerError", access = JsonProperty.Access.WRITE_ONLY)
    private InnerError innerError;

    /**
     * Get the specific identifier for the type of error encountered in the activity.
     *
     * @return the errorId value
     */
    public String errorId() {
        return this.errorId;
    }

    /**
     * Get the severity level of the failure. Possible values include: 'Warning', 'Error', 'Info', 'SevereWarning', 'Deprecated', 'UserWarning'.
     *
     * @return the severity value
     */
    public SeverityTypes severity() {
        return this.severity;
    }

    /**
     * Get the ultimate source of the failure (usually either SYSTEM or USER).
     *
     * @return the source value
     */
    public String source() {
        return this.source;
    }

    /**
     * Get the user friendly error message for the failure.
     *
     * @return the message value
     */
    public String message() {
        return this.message;
    }

    /**
     * Get the error message description.
     *
     * @return the description value
     */
    public String description() {
        return this.description;
    }

    /**
     * Get the details of the error message.
     *
     * @return the details value
     */
    public String details() {
        return this.details;
    }

    /**
     * Get the diagnostic error code.
     *
     * @return the diagnosticCode value
     */
    public Integer diagnosticCode() {
        return this.diagnosticCode;
    }

    /**
     * Get the component that failed.
     *
     * @return the component value
     */
    public String component() {
        return this.component;
    }

    /**
     * Get the recommended resolution for the failure, if any.
     *
     * @return the resolution value
     */
    public String resolution() {
        return this.resolution;
    }

    /**
     * Get the link to MSDN or Azure help for this type of error, if any.
     *
     * @return the helpLink value
     */
    public String helpLink() {
        return this.helpLink;
    }

    /**
     * Get the internal diagnostic stack trace if the user requesting the job error details has sufficient permissions it will be retrieved, otherwise it will be empty.
     *
     * @return the internalDiagnostics value
     */
    public String internalDiagnostics() {
        return this.internalDiagnostics;
    }

    /**
     * Get the inner error of this specific job error message, if any.
     *
     * @return the innerError value
     */
    public InnerError innerError() {
        return this.innerError;
    }

}
