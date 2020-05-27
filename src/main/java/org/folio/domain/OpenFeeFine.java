
package org.folio.domain;

import java.math.BigDecimal;
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
    "feeFineId",
    "balance",
    "feeFineTypeId"
})
public class OpenFeeFine {

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("feeFineId")
    @JsonPropertyDescription("A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/")
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private String feeFineId;
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
    @JsonProperty("feeFineTypeId")
    @JsonPropertyDescription("A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/")
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private String feeFineTypeId;

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

    public OpenFeeFine withFeeFineId(String feeFineId) {
        this.feeFineId = feeFineId;
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

    public OpenFeeFine withBalance(BigDecimal balance) {
        this.balance = balance;
        return this;
    }

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("feeFineTypeId")
    public String getFeeFineTypeId() {
        return feeFineTypeId;
    }

    /**
     * A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/
     *
     */
    @JsonProperty("feeFineTypeId")
    public void setFeeFineTypeId(String feeFineTypeId) {
        this.feeFineTypeId = feeFineTypeId;
    }

    public OpenFeeFine withFeeFineTypeId(String feeFineTypeId) {
        this.feeFineTypeId = feeFineTypeId;
        return this;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OpenFeeFine that = (OpenFeeFine) o;
        return Objects.equals(feeFineId, that.feeFineId) &&
          Objects.equals(balance, that.balance) &&
          Objects.equals(feeFineTypeId, that.feeFineTypeId);
    }

    @Override public int hashCode() {
        return Objects.hash(feeFineId, balance, feeFineTypeId);
    }
}
