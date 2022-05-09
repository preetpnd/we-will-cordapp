package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.WillContract;
import com.template.states.WillState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.List;

import static com.template.contracts.WillContract.WILL_CONTRACT_ID;

public class SearchVaultFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class SearchVaultFlowInitiator extends FlowLogic<SignedTransaction>{

        void searchForAllStates(){
            //------------------------------------Search for consumed States------------------------------------
            QueryCriteria consumedQueryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED);
            //Get All States of type will state which are consumed
            List<StateAndRef<WillState>> consumedWillStates = getServiceHub().getVaultService().queryBy(WillState.class, consumedQueryCriteria).getStates();

            if(consumedWillStates.size() <1 ){
                System.out.println("No Consumed Will States found");
            }else{
                System.out.println("Total Consumed Will States found: "+consumedWillStates.size());
            }

            consumedWillStates.forEach((x) ->{
                System.out.println("Will Id: "+ x.getState().getData().getWillId());
                System.out.println("Will Type: "+ x.getState().getData().getWillType());
                System.out.println("Will Details: "+ x.getState().getData().getWillDetails());
                System.out.println("Will Owner: "+ x.getState().getData().getOwner());
                System.out.println("Will Verifier: "+ x.getState().getData().getVerifier());
            });

            //------------------------------------Search for un consumed States------------------------------------
            QueryCriteria unConsumedQueryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            //Get All States of type will state which are consumed
            List<StateAndRef<WillState>> unConsumedWillStates = getServiceHub().getVaultService().queryBy(WillState.class, unConsumedQueryCriteria).getStates();

            if(unConsumedWillStates.size() <1 ){
                System.out.println("No Un-Consumed Will States found");
            }else{
                System.out.println("Total Un-Consumed Will States found: "+unConsumedWillStates.size());
            }

            unConsumedWillStates.forEach((x) ->{
                System.out.println("Will Id: "+ x.getState().getData().getWillId());
                System.out.println("Will Type: "+ x.getState().getData().getWillType());
                System.out.println("Will Details: "+ x.getState().getData().getWillDetails());
                System.out.println("Will Owner: "+ x.getState().getData().getOwner());
                System.out.println("Will Verifier: "+ x.getState().getData().getVerifier());
            });
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //Initiator logic goes here
            searchForAllStates();
            return null;
        }
    }

}