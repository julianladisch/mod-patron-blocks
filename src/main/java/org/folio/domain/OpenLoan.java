
package org.folio.domain;

import java.util.Objects;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Collection of open loans
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "loanId",
    "dueDate",
    "returnedDate",
    "recall"
})
public class OpenLoan {

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("loanId")
    @JsonPropertyDescription("A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/")
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private String loanId;
    /**
     * Loan due date
     *
     */
    @JsonProperty("dueDate")
    @JsonPropertyDescription("Loan due date")
    private String dueDate;
    /**
     * Loan return date
     *
     */
    @JsonProperty("returnedDate")
    @JsonPropertyDescription("Loan return date")
    private String returnedDate;
    /**
     * Due date has been changed by a recall
     *
     */
    @JsonProperty("recall")
    @JsonPropertyDescription("Due date has been changed by a recall")
    private Boolean recall;

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("loanId")
    public String getLoanId() {
        return loanId;
    }

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("loanId")
    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public OpenLoan withLoanId(String loanId) {
        this.loanId = loanId;
        return this;
    }

    /**
     * Loan due date
     *
     */
    @JsonProperty("dueDate")
    public String getDueDate() {
        return dueDate;
    }

    /**
     * Loan due date
     *
     */
    @JsonProperty("dueDate")
    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public OpenLoan withDueDate(String dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    /**
     * Loan return date
     *
     */
    @JsonProperty("returnedDate")
    public String getReturnedDate() {
        return returnedDate;
    }

    /**
     * Loan return date
     *
     */
    @JsonProperty("returnedDate")
    public void setReturnedDate(String returnedDate) {
        this.returnedDate = returnedDate;
    }

    public OpenLoan withReturnedDate(String returnedDate) {
        this.returnedDate = returnedDate;
        return this;
    }

    /**
     * Due date has been changed by a recall
     *
     */
    @JsonProperty("recall")
    public Boolean getRecall() {
        return recall;
    }

    /**
     * Due date has been changed by a recall
     *
     */
    @JsonProperty("recall")
    public void setRecall(Boolean recall) {
        this.recall = recall;
    }

    public OpenLoan withRecall(Boolean recall) {
        this.recall = recall;
        return this;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OpenLoan openLoan = (OpenLoan) o;
        return Objects.equals(loanId, openLoan.loanId) &&
          Objects.equals(dueDate, openLoan.dueDate) &&
          Objects.equals(returnedDate, openLoan.returnedDate) &&
          Objects.equals(recall, openLoan.recall);
    }

    @Override public int hashCode() {
        return Objects.hash(loanId, dueDate, returnedDate, recall);
    }
}
