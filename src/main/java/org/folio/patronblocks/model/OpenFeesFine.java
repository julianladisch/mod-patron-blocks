
package org.folio.patronblocks.model;

import java.math.BigDecimal;

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
    "accountId",
    "balance",
    "feeFineId"
})
public class OpenFeesFine {

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("accountId")
    @JsonPropertyDescription("A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/")
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private String accountId;
    /**
     * Balance
     *
     */
    @JsonProperty("balance")
    @JsonPropertyDescription("Balance")
    private BigDecimal balance;
    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("feeFineId")
    @JsonPropertyDescription("A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/")
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private String feeFineId;

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("accountId")
    public String getAccountId() {
        return accountId;
    }

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("accountId")
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public OpenFeesFine withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    /**
     * Balance
     *
     */
    @JsonProperty("balance")
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Balance
     *
     */
    @JsonProperty("balance")
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public OpenFeesFine withBalance(BigDecimal balance) {
        this.balance = balance;
        return this;
    }

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("feeFineId")
    public String getFeeFineId() {
        return feeFineId;
    }

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("feeFineId")
    public void setFeeFineId(String feeFineId) {
        this.feeFineId = feeFineId;
    }

    public OpenFeesFine withFeeFineId(String feeFineId) {
        this.feeFineId = feeFineId;
        return this;
    }

}
