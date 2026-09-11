package com.tradingsimulator.processengine;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;

final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

	private final ThreadLocal<Holder> bound = new ThreadLocal<>();
	private int commits;
	private int rollbacks;

	int committed() {
		return commits;
	}

	int rolledBack() {
		return rollbacks;
	}

	@Override
	protected Object doGetTransaction() {
		return new Transaction(bound.get());
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		return ((Transaction) transaction).holder != null;
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		Holder holder = new Holder();
		((Transaction) transaction).holder = holder;
		bound.set(holder);
	}

	@Override
	protected Object doSuspend(Object transaction) {
		((Transaction) transaction).holder = null;
		Holder suspended = bound.get();
		bound.remove();
		return suspended;
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		bound.set((Holder) suspendedResources);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		commits++;
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		rollbacks++;
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		((Transaction) status.getTransaction()).holder.rollbackOnly = true;
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		bound.remove();
	}

	private static final class Holder {
		private boolean rollbackOnly;
	}

	private static final class Transaction implements SmartTransactionObject {

		private Holder holder;

		private Transaction(Holder holder) {
			this.holder = holder;
		}

		@Override
		public boolean isRollbackOnly() {
			return holder != null && holder.rollbackOnly;
		}

		@Override
		public void flush() {
		}
	}
}
