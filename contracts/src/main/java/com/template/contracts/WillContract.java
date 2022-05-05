package com.template.contracts;

import com.template.states.WillState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

// ************
// * Contract *
// ************
public class WillContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String WILL_CONTRACT_ID = "com.template.contracts.WillContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(@NotNull LedgerTransaction tx)  throws IllegalArgumentException{
        //Only allow one command to be executed at a time
        if(tx.getCommands().size() != 1) throw new IllegalArgumentException("Transaction can only have one command");

        //Fetch the command
        Command command  = tx.getCommand(0);
        CommandData commandType = command.getValue();
        List<PublicKey> requiredSigners = command.getSigners(); //get all signers

        if(commandType instanceof GenerateWill) {
            //GenerateWill Logic

            //Shape rules = governs what input and output are allowed
            //For this case we want zero inputs and 1 outputs as this is the issuer
            if(tx.getInputs().size() != 0)
                throw new IllegalArgumentException("The Issuer cannot have inputs in generate will flow");

            if(tx.getOutputs().size() != 1)
                throw new IllegalArgumentException("There can only be one output in generate will flow");

            //Content rules = what the contents of input and output would be
            //Validate if output is of type of will state and the will type is valid
            ContractState outputState = tx.getOutput(0);
            if(!(outputState instanceof WillState))
                throw new IllegalArgumentException("The output of Generate will flow should be of Will State");

            WillState willState = (WillState)outputState;
            if (!(willState.getWillType().equalsIgnoreCase("Estate Will"))){
                throw new IllegalArgumentException("The Will Type can only be a Estate Will");
            }

            //Signing Rules = who all would be signing the transaction
            //To make sure Issuer/requester signs are captured
            Party requester = willState.getRequester();
            PublicKey requesterKey = requester.getOwningKey();

            if(!requiredSigners.contains(requesterKey))
                throw new IllegalArgumentException("Requester has to sign the request to generate the will");
        }
    }

    // Used to indicate the transaction's intent.
    public static class GenerateWill implements CommandData {}
}