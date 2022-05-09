package com.template.states;

import com.template.contracts.WillContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(WillContract.class)
public class WillState implements ContractState {

    //private variables
    private final String willId;
    private final String willType;
    private final String willDetails;
    private final String willStatus;
    private final Party owner;
    private final Party verifier;

    /* Constructor of your Corda state */
    public WillState(String willId, String willType, String willDetails,String willStatus, Party owner, Party verifier) {
        this.willId = willId;
        this.willType = willType;
        this.willDetails = willDetails;
        this.willStatus = willStatus;
        this.owner = owner;
        this.verifier = verifier;
    }

    //getters
    public String getWillId() { return willId; }
    public String getWillType() { return willType; }
    public String getWillDetails() { return willDetails; }
    public String getWillStatus() { return willStatus; }
    public Party getOwner() { return owner; }
    public Party getVerifier() { return verifier; }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner,verifier);
    }
}