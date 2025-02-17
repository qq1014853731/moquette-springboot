/*
 * Copyright (c) 2012-2018 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.moquette.broker.subscriptions;

import java.util.*;

class CNode {

    private Token token;
    private List<INode> children;
    Set<Subscription> subscriptions;

    CNode() {
        this.children = new ArrayList<>();
        this.subscriptions = new HashSet<>();
    }

    //Copy constructor
    private CNode(Token token, List<INode> children, Set<Subscription> subscriptions) {
        this.token = token; // keep reference, root comparison in directory logic relies on it for now.
        this.subscriptions = new HashSet<>(subscriptions);
        this.children = new ArrayList<>(children);
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    boolean anyChildrenMatch(Token token) {
        for (INode iNode : children) {
            final CNode child = iNode.mainNode();
            if (child.equalsToken(token)) {
                return true;
            }
        }
        return false;
    }

    List<INode> allChildren() {
        return this.children;
    }

    INode childOf(Token token) {
        for (INode iNode : children) {
            final CNode child = iNode.mainNode();
            if (child.equalsToken(token)) {
                return iNode;
            }
        }
        throw new IllegalArgumentException("Asked for a token that doesn't exists in any child [" + token + "]");
    }

    private boolean equalsToken(Token token) {
        return token != null && this.token != null && this.token.equals(token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }

    CNode copy() {
        return new CNode(this.token, this.children, this.subscriptions);
    }

    public void add(INode newINode) {
        this.children.add(newINode);
    }

    public void remove(INode node) {
        this.children.remove(node);
    }

    CNode addSubscription(Subscription newSubscription) {
        // if already contains one with same topic and same client, keep that with higher QoS
        if (subscriptions.contains(newSubscription)) {
            final Subscription existing = subscriptions.stream()
                .filter(s -> s.equals(newSubscription))
                .findFirst().get();
            if (existing.getRequestedQos().value() < newSubscription.getRequestedQos().value()) {
                subscriptions.remove(existing);
                subscriptions.add(new Subscription(newSubscription));
            }
        } else {
            this.subscriptions.add(new Subscription(newSubscription));
        }
        return this;
    }

    /**
     * @return true iff the subscriptions contained in this node are owned by clientId
     *   AND at least one subscription is actually present for that clientId
     * */
    boolean containsOnly(String clientId) {
        for (Subscription sub : this.subscriptions) {
            if (!sub.getClientId().equals(clientId)) {
                return false;
            }
        }
        return !this.subscriptions.isEmpty();
    }

    //TODO this is equivalent to negate(containsOnly(clientId))
    public boolean contains(String clientId) {
        for (Subscription sub : this.subscriptions) {
            if (sub.getClientId().equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    void removeSubscriptionsFor(String clientId) {
        Set<Subscription> toRemove = new HashSet<>();
        for (Subscription sub : this.subscriptions) {
            if (sub.getClientId().equals(clientId)) {
                toRemove.add(sub);
            }
        }
        this.subscriptions.removeAll(toRemove);
    }
}
