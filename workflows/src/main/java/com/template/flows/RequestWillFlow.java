package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.WillContract;
import com.template.states.WillState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;

import static com.template.contracts.WillContract.WILL_CONTRACT_ID;

public class RequestWillFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class RequestWillFlowInitiator extends FlowLogic<SignedTransaction>{

        //we do not need the issuer as he is running the flow
        private String willId;
        private String willType;
        private String willDetails;
        private Party verifier;

        public RequestWillFlowInitiator(String willId, String willType, String willDetails, Party verifier) {
            this.willId = willId;
            this.willType = willType;
            this.willDetails = willDetails;
            this.verifier = verifier;
        }

        //adding steps to create a trail
        private final ProgressTracker.Step VALIDATING_OWNER = new ProgressTracker.Step("Validating the Owner of transaction");
        private final ProgressTracker.Step RETRIEVING_NOTARY = new ProgressTracker.Step("Retrieving the Notary");
        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating Transaction");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing the transaction with private key");
        private final ProgressTracker.Step COUNTERPARTY_SESSION = new ProgressTracker.Step("Sending the flow to Verifier");
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

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //Initiator logic goes here

            //Do an identity check to restrict the owner
            progresstracker.setCurrentStep(VALIDATING_OWNER);
            if(getOurIdentity().getName().getOrganisation().equalsIgnoreCase("WillOwner")) {
                System.out.println("Identity Verified");
            }
            else{
                throw new FlowException("Identity only be WillOwner");
            }

            //***in corda we build transactions using transactionbuilder, we need to notary, command and output state to transaction***
            //Retrieve the notary identity from the network map
            progresstracker.setCurrentStep(RETRIEVING_NOTARY);
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            //Create the transaction components(Input and Outputs)
            //create output state, it has 3 fields including verifier for this use case

            WillState outputState = new WillState(this.willId, this.willType, this.willDetails,"Verification Requested",  getOurIdentity(), this.verifier);

            progresstracker.setCurrentStep(GENERATING_TRANSACTION);
            TransactionBuilder txbuilder = new TransactionBuilder(notary);
            txbuilder.addOutputState(outputState, WILL_CONTRACT_ID);
            txbuilder.addCommand(new WillContract.RequestWill(), getOurIdentity().getOwningKey());

            //Signing the transaction
            progresstracker.setCurrentStep(SIGNING_TRANSACTION);
            SignedTransaction willTx = getServiceHub().signInitialTransaction(txbuilder);

            //Send transaction to counterparty, communication is done using session
            //Create session with counterparty
            progresstracker.setCurrentStep(COUNTERPARTY_SESSION);
            FlowSession otherPartySession = initiateFlow(verifier);

            /*// Obtaining the counterparty's signature.
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                    willTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));*/

            //Verify transaction and send to Notary amd once it's done commit the transaction(can be done using subflow to finalize the transaction)
            //Finalize the transaction
            progresstracker.setCurrentStep(FINALIZING_TRANSACTION);
            return subFlow(new FinalityFlow(willTx, otherPartySession));
        }
    }



    @InitiatedBy(RequestWillFlowInitiator.class)
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
               System.out.println("Received Will request for verification");
               return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }

}