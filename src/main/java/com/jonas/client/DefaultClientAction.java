package com.jonas.client;

/**
 * 【 enter the class description 】
 *
 * @author shenjy 2019/03/04
 */
public class DefaultClientAction implements ClientAction {

    @Override
    public void doAction(Object obj, Client client) {
        System.out.println("处理：\t" + obj.toString());
    }

}
