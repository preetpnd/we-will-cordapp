package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.WillContract;
import com.template.states.WillState;
import jdk.nashorn.internal.ir.annotations.Ignore;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.IQueryCriteriaParser;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import javax.persistence.criteria.Predicate;
import java.util.Collection;
import java.util.List;

import static com.template.contracts.WillContract.WILL_CONTRACT_ID;

public class GenerateWillFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class GenerateWillFlowInitiator extends FlowLogic<SignedTransaction>{

        StateAndRef<WillState> matchedState = null;
        private String willId;
        private Party owner;

        public GenerateWillFlowInitiator(String willId, Party owner) {
            this.willId = willId;
            this.owner = owner;
        }

        //adding steps to create a trail
        private final ProgressTracker.Step VALIDATING_OWNER = new ProgressTracker.Step("Validating the Owner of transaction");
        private final ProgressTracker.Step RETRIEVING_NOTARY = new ProgressTracker.Step("Retrieving the Notary");
        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating Transaction");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing the transaction with private key");
        private final ProgressTracker.Step COUNTERPARTY_SESSION = new ProgressTracker.Step("Sending the flow to Owner");
        private final ProgressTracker.Step FINALIZING_TRANSACTION = new ProgressTracker.Step("Obtaining Notary signature and committing the transaction");

        private final ProgressTracker progresstracker = new ProgressTracker(
                VALIDATING_OWNER,
                RETRIEVING_NOTARY,
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                COUNTERPARTY_SESSION,
                FINALIZING_TRANSACTION
        );

        @Override
        public ProgressTracker getProgressTracker() {return progresstracker;}

        // Check for existing WillId Starts
        private StateAndRef<WillState> CheckForWillID() throws FlowException {
            //Check for all values that are unconsumed in vault
            QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

            //Get All States of type will state which are unconsumed
            List<StateAndRef<WillState>> willStates = getServiceHub().getVaultService().queryBy(WillState.class, generalCriteria).getStates();
            boolean inputFound = false;

            for(int i=0; i < willStates.size(); i++){
                if(willStates.get(i).getState().getData().getWillId().equals(willId)){
                    inputFound = true;
                    matchedState = willStates.get(i);
                }
            }

            if(inputFound){
                System.out.println("\nInput found");
            }else{
                System.out.println("\nInput not found");
                throw new FlowException();
            }
            return matchedState;
        }
        // Check for existing WillId Ends
        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //Initiator logic goes here

            //Do an identity check to restrict the owner
            progresstracker.setCurrentStep(VALIDATING_OWNER);
            if(getOurIdentity().getName().getOrganisation().equalsIgnoreCase("WillCertifier")) {
                System.out.println("Identity Verified");
            }
            else{
                throw new FlowException("Identity only be Will Certifier");
            }

            //***in corda we build transactions using transactionbuilder, we need to notary, command and output state to transaction***
            //Retrieve the notary identity from the network map
            progresstracker.setCurrentStep(RETRIEVING_NOTARY);
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);


            //Create the transaction components(Input and Outputs)
            //need to create an input state: retrieve from vault
            StateAndRef<WillState> inputState = CheckForWillID();

            //fetch will details and type from input state
            //WillState outputState = new WillState(this.willId, this.willType, this.willDetails, getOurIdentity(), this.owner);
            WillState outputState = new WillState(this.willId, inputState.getState().getData().getWillType(), inputState.getState().getData().getWillDetails(), "Will Generated", getOurIdentity(), this.owner);
            Command cmd = new Command(new WillContract.GenerateWill(), getOurIdentity().getOwningKey());

            progresstracker.setCurrentStep(GENERATING_TRANSACTION);
            TransactionBuilder txbuilder = new TransactionBuilder(notary);
            txbuilder.addOutputState(outputState, WILL_CONTRACT_ID);
            txbuilder.addCommand(cmd);

            txbuilder.addInputState(inputState);

            //Signing the transaction
            progresstracker.setCurrentStep(SIGNING_TRANSACTION);
            SignedTransaction willTx = getServiceHub().signInitialTransaction(txbuilder);

            //Send transaction to counterparty, communication is done using session
            //Create session with counterparty
            progresstracker.setCurrentStep(COUNTERPARTY_SESSION);
            FlowSession otherPartySession = initiateFlow(owner);
            //FlowSession verifierPartySession = initiateFlow(issuer);


            //Verify transaction and send to Notary amd once it's done commit the transaction(can be done using subflow to finalize the transaction)
            //Finalize the transaction
            progresstracker.setCurrentStep(FINALIZING_TRANSACTION);
            return subFlow(new FinalityFlow(willTx, otherPartySession));
        }
    }



    @InitiatedBy(GenerateWillFlowInitiator.class)
    public static class GenerateWillFlowResponder extends FlowLogic<SignedTransaction>{
        //private variable
        private FlowSession otherPartySession;

        //Constructor
        public GenerateWillFlowResponder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
               System.out.println("Verified and updated the requested Will");
               return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }

}