
package org.folio.domain;

import java.util.Date;

import javax.validation.constraints.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private Date dueDate;
    /**
     * Loan return date
     *
     */
    @JsonProperty("returnedDate")
    @JsonPropertyDescription("Loan return date")
    private Date returnedDate;
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
    public Date getDueDate() {
        return dueDate;
    }

    /**
     * Loan due date
     *
     */
    @JsonProperty("dueDate")
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public OpenLoan withDueDate(Date dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    /**
     * Loan return date
     *
     */
    @JsonProperty("returnedDate")
    public Date getReturnedDate() {
        return returnedDate;
    }

    /**
     * Loan return date
     *
     */
    @JsonProperty("returnedDate")
    public void setReturnedDate(Date returnedDate) {
        this.returnedDate = returnedDate;
    }

    public OpenLoan withReturnedDate(Date returnedDate) {
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

    @JsonIgnore
    public boolean isOverdue() {
        return ObjectUtils.allNotNull(dueDate, returnedDate)
          && returnedDate.after(dueDate);
    }

    @JsonIgnore
    public int getOverdueDays() {
        return isOverdue()
          ? Days.daysBetween(
            new DateTime(dueDate).toLocalDate(), new DateTime(returnedDate).toLocalDate()).getDays()
          : 0;
    }

}
