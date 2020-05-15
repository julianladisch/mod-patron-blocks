
package org.folio.patronblocks.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * User Summary object allows to check each of the block conditions for a patron
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "userId",
    "outstandingFeeFineBalance",
    "numberOfOpenFeesFinesForLostItems",
    "numberOfLostItems",
    "openLoans",
    "openFeesFines",
    "metadata"
})
public class UserSummary {

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     * (Required)
     *
     */
    @JsonProperty("id")
    @JsonPropertyDescription("A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/")
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    @NotNull
    private String id;
    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("userId")
    @JsonPropertyDescription("A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/")
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private String userId;
    /**
     * Outstanding fee/fine balance
     *
     */
    @JsonProperty("outstandingFeeFineBalance")
    @JsonPropertyDescription("Outstanding fee/fine balance")
    private BigDecimal outstandingFeeFineBalance;
    /**
     * Number of open fees/fines for lost items
     *
     */
    @JsonProperty("numberOfOpenFeesFinesForLostItems")
    @JsonPropertyDescription("Number of open fees/fines for lost items")
    private Integer numberOfOpenFeesFinesForLostItems;
    /**
     * Number of lost items
     *
     */
    @JsonProperty("numberOfLostItems")
    @JsonPropertyDescription("Number of lost items")
    private Integer numberOfLostItems;
    @JsonProperty("openLoans")
    @Valid
    private List<OpenLoan> openLoans = new ArrayList<OpenLoan>();
    @JsonProperty("openFeesFines")
    @Valid
    private List<OpenFeeFine> openFeeFines = new ArrayList<OpenFeeFine>();

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     * (Required)
     *
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     * (Required)
     *
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public UserSummary withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("userId")
    public String getUserId() {
        return userId;
    }

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public UserSummary withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Outstanding fee/fine balance
     *
     */
    @JsonProperty("outstandingFeeFineBalance")
    public BigDecimal getOutstandingFeeFineBalance() {
        return outstandingFeeFineBalance;
    }

    /**
     * Outstanding fee/fine balance
     *
     */
    @JsonProperty("outstandingFeeFineBalance")
    public void setOutstandingFeeFineBalance(BigDecimal outstandingFeeFineBalance) {
        this.outstandingFeeFineBalance = outstandingFeeFineBalance;
    }

    public UserSummary withOutstandingFeeFineBalance(BigDecimal outstandingFeeFineBalance) {
        this.outstandingFeeFineBalance = outstandingFeeFineBalance;
        return this;
    }

    /**
     * Number of open fees/fines for lost items
     *
     */
    @JsonProperty("numberOfOpenFeesFinesForLostItems")
    public Integer getNumberOfOpenFeesFinesForLostItems() {
        return numberOfOpenFeesFinesForLostItems;
    }

    /**
     * Number of open fees/fines for lost items
     *
     */
    @JsonProperty("numberOfOpenFeesFinesForLostItems")
    public void setNumberOfOpenFeesFinesForLostItems(Integer numberOfOpenFeesFinesForLostItems) {
        this.numberOfOpenFeesFinesForLostItems = numberOfOpenFeesFinesForLostItems;
    }

    public UserSummary withNumberOfOpenFeesFinesForLostItems(Integer numberOfOpenFeesFinesForLostItems) {
        this.numberOfOpenFeesFinesForLostItems = numberOfOpenFeesFinesForLostItems;
        return this;
    }

    /**
     * Number of lost items
     *
     */
    @JsonProperty("numberOfLostItems")
    public Integer getNumberOfLostItems() {
        return numberOfLostItems;
    }

    /**
     * Number of lost items
     *
     */
    @JsonProperty("numberOfLostItems")
    public void setNumberOfLostItems(Integer numberOfLostItems) {
        this.numberOfLostItems = numberOfLostItems;
    }

    public UserSummary withNumberOfLostItems(Integer numberOfLostItems) {
        this.numberOfLostItems = numberOfLostItems;
        return this;
    }

    @JsonProperty("openLoans")
    public List<OpenLoan> getOpenLoans() {
        return openLoans;
    }

    @JsonProperty("openLoans")
    public void setOpenLoans(List<OpenLoan> openLoans) {
        this.openLoans = openLoans;
    }

    public UserSummary withOpenLoans(List<OpenLoan> openLoans) {
        this.openLoans = openLoans;
        return this;
    }

    @JsonProperty("openFeesFines")
    public List<OpenFeeFine> getOpenFeeFines() {
        return openFeeFines;
    }

    @JsonProperty("openFeesFines")
    public void setOpenFeeFines(List<OpenFeeFine> openFeeFines) {
        this.openFeeFines = openFeeFines;
    }

    public UserSummary withOpenFeesFines(List<OpenFeeFine> openFeeFines) {
        this.openFeeFines = openFeeFines;
        return this;
    }
}
