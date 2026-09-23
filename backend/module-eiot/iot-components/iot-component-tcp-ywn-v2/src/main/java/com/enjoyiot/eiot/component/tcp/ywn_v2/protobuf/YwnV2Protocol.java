package com.enjoyiot.eiot.component.tcp.ywn_v2.protobuf;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * YWN V2 协议 Protobuf 消息定义（手写轻量实现，wire format 与 protoc 生成代码一致）
 *
 * 消息类型枚举：
 *   MESSAGE_TYPE_REGISTER  = 1  注册
 *   MESSAGE_TYPE_HEARTBEAT = 2  心跳
 *   MESSAGE_TYPE_EVENT     = 3  事件上报
 *   MESSAGE_TYPE_CONTROL   = 4  控制下发
 *
 * 对应 ywn_v2.proto 定义：
 *   message Message     { bool ok = 1; string msg = 2; }
 *   message RegisterReq { string gateway = 1; }
 *   message EventReq    { uint32 event=1; uint32 host=2; uint32 loop=3; uint32 once=4;
 *                         uint32 devtype=5; string twice=6; bool zongxian=7; string device_code=8; }
 *   message ControlReq  { uint32 ctrl=1; uint32 host=2; uint32 loop=3; uint32 once=4;
 *                         uint32 devtype=5; string twice=6; bool zongxian=7; }
 */
public final class YwnV2Protocol {

    private YwnV2Protocol() {}

    /**
     * 通用确认消息
     */
    public static final class Message {

        private final boolean ok;
        private final String msg;

        private Message(Builder builder) {
            this.ok = builder.ok;
            this.msg = builder.msg;
        }

        public boolean getOk() { return ok; }
        public String getMsg() { return msg; }

        public static Builder newBuilder() { return new Builder(); }

        /** 编码为 protobuf 二进制 */
        public byte[] toByteArray() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CodedOutputStream cos = CodedOutputStream.newInstance(bos);
                if (ok) {
                    cos.writeBool(1, true);
                }
                if (!msg.isEmpty()) {
                    cos.writeString(2, msg);
                }
                cos.flush();
                return bos.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("Message 编码失败", e);
            }
        }

        /** 从 protobuf 二进制解码 */
        public static Message parseFrom(byte[] data) throws InvalidProtocolBufferException {
            if (data == null || data.length == 0) {
                return newBuilder().build();
            }
            try {
                CodedInputStream cis = CodedInputStream.newInstance(data);
                Builder b = newBuilder();
                while (true) {
                    int tag = cis.readTag();
                    if (tag == 0) {
                        break;
                    }
                    switch (tag) {
                        case 8:  b.setOk(cis.readBool()); break;              // field 1: bool ok
                        case 18: b.setMsg(cis.readStringRequireUtf8()); break; // field 2: string msg
                        default: cis.skipField(tag); break;
                    }
                }
                return b.build();
            } catch (InvalidProtocolBufferException e) {
                throw e;
            } catch (IOException e) {
                throw new InvalidProtocolBufferException(e);
            }
        }

        public static final class Builder {
            private boolean ok;
            private String msg = "";

            private Builder() {}

            public Builder setOk(boolean value) { this.ok = value; return this; }
            public Builder setMsg(String value) { this.msg = (value == null ? "" : value); return this; }
            public Message build() { return new Message(this); }
        }
    }

    /**
     * 注册请求
     */
    public static final class RegisterReq {

        private final String gateway;

        private RegisterReq(Builder builder) {
            this.gateway = builder.gateway;
        }

        public String getGateway() { return gateway; }

        public static Builder newBuilder() { return new Builder(); }

        /** 编码为 protobuf 二进制 */
        public byte[] toByteArray() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CodedOutputStream cos = CodedOutputStream.newInstance(bos);
                if (!gateway.isEmpty()) {
                    cos.writeString(1, gateway);
                }
                cos.flush();
                return bos.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("RegisterReq 编码失败", e);
            }
        }

        /** 从 protobuf 二进制解码 */
        public static RegisterReq parseFrom(byte[] data) throws InvalidProtocolBufferException {
            if (data == null || data.length == 0) {
                return newBuilder().build();
            }
            try {
                CodedInputStream cis = CodedInputStream.newInstance(data);
                Builder b = newBuilder();
                while (true) {
                    int tag = cis.readTag();
                    if (tag == 0) {
                        break;
                    }
                    switch (tag) {
                        case 10: b.setGateway(cis.readStringRequireUtf8()); break; // field 1: string gateway
                        default: cis.skipField(tag); break;
                    }
                }
                return b.build();
            } catch (InvalidProtocolBufferException e) {
                throw e;
            } catch (IOException e) {
                throw new InvalidProtocolBufferException(e);
            }
        }

        public static final class Builder {
            private String gateway = "";

            private Builder() {}

            public Builder setGateway(String value) { this.gateway = (value == null ? "" : value); return this; }
            public RegisterReq build() { return new RegisterReq(this); }
        }
    }
    /**
     * 事件上报
     */
    public static final class EventReq {

        private final int event;
        private final int host;
        private final int loop;
        private final int once;
        private final int devtype;
        private final String twice;
        private final boolean zongxian;
        private final String deviceCode;

        private EventReq(Builder builder) {
            this.event = builder.event;
            this.host = builder.host;
            this.loop = builder.loop;
            this.once = builder.once;
            this.devtype = builder.devtype;
            this.twice = builder.twice;
            this.zongxian = builder.zongxian;
            this.deviceCode = builder.deviceCode;
        }

        public int getEvent() { return event; }
        public int getHost() { return host; }
        public int getLoop() { return loop; }
        public int getOnce() { return once; }
        public int getDevtype() { return devtype; }
        public String getTwice() { return twice; }
        public boolean getZongxian() { return zongxian; }
        public String getDeviceCode() { return deviceCode; }

        public static Builder newBuilder() { return new Builder(); }

        /** 编码为 protobuf 二进制 */
        public byte[] toByteArray() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CodedOutputStream cos = CodedOutputStream.newInstance(bos);
                if (event != 0) {
                    cos.writeUInt32(1, event);
                }
                if (host != 0) {
                    cos.writeUInt32(2, host);
                }
                if (loop != 0) {
                    cos.writeUInt32(3, loop);
                }
                if (once != 0) {
                    cos.writeUInt32(4, once);
                }
                if (devtype != 0) {
                    cos.writeUInt32(5, devtype);
                }
                if (!twice.isEmpty()) {
                    cos.writeString(6, twice);
                }
                if (zongxian) {
                    cos.writeBool(7, true);
                }
                if (!deviceCode.isEmpty()) {
                    cos.writeString(8, deviceCode);
                }
                cos.flush();
                return bos.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("EventReq 编码失败", e);
            }
        }

        /** 从 protobuf 二进制解码 */
        public static EventReq parseFrom(byte[] data) throws InvalidProtocolBufferException {
            if (data == null || data.length == 0) {
                return newBuilder().build();
            }
            try {
                CodedInputStream cis = CodedInputStream.newInstance(data);
                Builder b = newBuilder();
                while (true) {
                    int tag = cis.readTag();
                    if (tag == 0) {
                        break;
                    }
                    switch (tag) {
                        case 8:  b.setEvent(cis.readUInt32()); break;      // field 1: uint32 event
                        case 16: b.setHost(cis.readUInt32()); break;       // field 2: uint32 host
                        case 24: b.setLoop(cis.readUInt32()); break;       // field 3: uint32 loop
                        case 32: b.setOnce(cis.readUInt32()); break;       // field 4: uint32 once
                        case 40: b.setDevtype(cis.readUInt32()); break;    // field 5: uint32 devtype
                        case 50: b.setTwice(cis.readStringRequireUtf8()); break; // field 6: string twice
                        case 56: b.setZongxian(cis.readBool()); break;     // field 7: bool zongxian
                        case 66: b.setDeviceCode(cis.readStringRequireUtf8()); break; // field 8: string device_code
                        default: cis.skipField(tag); break;
                    }
                }
                return b.build();
            } catch (InvalidProtocolBufferException e) {
                throw e;
            } catch (IOException e) {
                throw new InvalidProtocolBufferException(e);
            }
        }

        public static final class Builder {
            private int event;
            private int host;
            private int loop;
            private int once;
            private int devtype;
            private String twice = "";
            private boolean zongxian;
            private String deviceCode = "";

            private Builder() {}

            public Builder setEvent(int value) { this.event = value; return this; }
            public Builder setHost(int value) { this.host = value; return this; }
            public Builder setLoop(int value) { this.loop = value; return this; }
            public Builder setOnce(int value) { this.once = value; return this; }
            public Builder setDevtype(int value) { this.devtype = value; return this; }
            public Builder setTwice(String value) { this.twice = (value == null ? "" : value); return this; }
            public Builder setZongxian(boolean value) { this.zongxian = value; return this; }
            public Builder setDeviceCode(String value) { this.deviceCode = (value == null ? "" : value); return this; }
            public EventReq build() { return new EventReq(this); }
        }
    }

    /**
     * 控制下发
     */
    public static final class ControlReq {

        private final int ctrl;
        private final int host;
        private final int loop;
        private final int once;
        private final int devtype;
        private final String twice;
        private final boolean zongxian;

        private ControlReq(Builder builder) {
            this.ctrl = builder.ctrl;
            this.host = builder.host;
            this.loop = builder.loop;
            this.once = builder.once;
            this.devtype = builder.devtype;
            this.twice = builder.twice;
            this.zongxian = builder.zongxian;
        }

        public int getCtrl() { return ctrl; }
        public int getHost() { return host; }
        public int getLoop() { return loop; }
        public int getOnce() { return once; }
        public int getDevtype() { return devtype; }
        public String getTwice() { return twice; }
        public boolean getZongxian() { return zongxian; }

        public static Builder newBuilder() { return new Builder(); }

        /** 编码为 protobuf 二进制 */
        public byte[] toByteArray() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CodedOutputStream cos = CodedOutputStream.newInstance(bos);
                if (ctrl != 0) {
                    cos.writeUInt32(1, ctrl);
                }
                if (host != 0) {
                    cos.writeUInt32(2, host);
                }
                if (loop != 0) {
                    cos.writeUInt32(3, loop);
                }
                if (once != 0) {
                    cos.writeUInt32(4, once);
                }
                if (devtype != 0) {
                    cos.writeUInt32(5, devtype);
                }
                if (!twice.isEmpty()) {
                    cos.writeString(6, twice);
                }
                if (zongxian) {
                    cos.writeBool(7, true);
                }
                cos.flush();
                return bos.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("ControlReq 编码失败", e);
            }
        }

        /** 从 protobuf 二进制解码 */
        public static ControlReq parseFrom(byte[] data) throws InvalidProtocolBufferException {
            if (data == null || data.length == 0) {
                return newBuilder().build();
            }
            try {
                CodedInputStream cis = CodedInputStream.newInstance(data);
                Builder b = newBuilder();
                while (true) {
                    int tag = cis.readTag();
                    if (tag == 0) {
                        break;
                    }
                    switch (tag) {
                        case 8:  b.setCtrl(cis.readUInt32()); break;        // field 1: uint32 ctrl
                        case 16: b.setHost(cis.readUInt32()); break;        // field 2: uint32 host
                        case 24: b.setLoop(cis.readUInt32()); break;        // field 3: uint32 loop
                        case 32: b.setOnce(cis.readUInt32()); break;        // field 4: uint32 once
                        case 40: b.setDevtype(cis.readUInt32()); break;     // field 5: uint32 devtype
                        case 50: b.setTwice(cis.readStringRequireUtf8()); break; // field 6: string twice
                        case 56: b.setZongxian(cis.readBool()); break;      // field 7: bool zongxian
                        default: cis.skipField(tag); break;
                    }
                }
                return b.build();
            } catch (InvalidProtocolBufferException e) {
                throw e;
            } catch (IOException e) {
                throw new InvalidProtocolBufferException(e);
            }
        }

        public static final class Builder {
            private int ctrl;
            private int host;
            private int loop;
            private int once;
            private int devtype;
            private String twice = "";
            private boolean zongxian;

            private Builder() {}

            public Builder setCtrl(int value) { this.ctrl = value; return this; }
            public Builder setHost(int value) { this.host = value; return this; }
            public Builder setLoop(int value) { this.loop = value; return this; }
            public Builder setOnce(int value) { this.once = value; return this; }
            public Builder setDevtype(int value) { this.devtype = value; return this; }
            public Builder setTwice(String value) { this.twice = (value == null ? "" : value); return this; }
            public Builder setZongxian(boolean value) { this.zongxian = value; return this; }
            public ControlReq build() { return new ControlReq(this); }
        }
    }
}