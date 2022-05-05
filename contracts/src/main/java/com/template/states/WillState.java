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
    private final Party requester;
    private final Party verifier;

    /* Constructor of your Corda state */
    public WillState(String willId, String willType, String willDetails, Party requester, Party verifier) {
        this.willId = willId;
        this.willType = willType;
        this.willDetails = willDetails;
        this.requester = requester;
        this.verifier = verifier;
    }

    //getters
    public String getWillId() { return willId; }
    public String getWillType() { return willType; }
    public String getWillDetails() { return willDetails; }
    public Party getRequester() { return requester; }
    public Party getVerifier() { return verifier; }


    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(requester,verifier);
    }
}