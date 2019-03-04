package com.jonas.server;

/**
 * 【 enter the class description 】
 *
 * @author shenjy 2019/03/04
 */
public class DefaultServerAction implements ServerAction {

    @Override
    public Object doAction(Object obj, Server server) {
        System.out.println("处理并返回：" + obj);
        return obj;
    }

}
