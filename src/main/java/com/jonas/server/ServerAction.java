package com.jonas.server;

import com.jonas.client.Client;

/**
 * 【 enter the class description 】
 *
 * @author shenjy 2019/03/04
 */
public interface ServerAction {
    Object doAction(Object obj, Server server);
}
