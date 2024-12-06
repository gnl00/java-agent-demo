package one.demo;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Test attach api agentmain
 */
public class DemoAttachClient {
    public static void main(String[] args) {
        // com.sun.tools.attach.VirtualMachine virtualMachine = com.sun.tools.attach.VirtualMachine.attach("");
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        for (VirtualMachineDescriptor vmd : list) {
            System.out.println(vmd.displayName());
            if ("one.demo.DemoClass".equals(vmd.displayName())) {
                VirtualMachine vm = null;
                try {
                    vm = VirtualMachine.attach(vmd.id());
                    vm.loadAgent("demo-agent-1.0-SNAPSHOT.jar");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (null != vm) {
                            vm.detach();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
