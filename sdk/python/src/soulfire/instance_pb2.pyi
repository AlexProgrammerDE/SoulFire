import datetime

from soulfire import common_pb2 as _common_pb2
from soulfire import api_docs_pb2 as _api_docs_pb2
from soulfire import bot_pb2 as _bot_pb2
from google.api import annotations_pb2 as _annotations_pb2
from google.api import field_behavior_pb2 as _field_behavior_pb2
from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf import struct_pb2 as _struct_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class BotAuthentication(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    BOT_AUTHENTICATION_UNSPECIFIED: _ClassVar[BotAuthentication]
    BOT_AUTHENTICATION_OFFLINE: _ClassVar[BotAuthentication]
    BOT_AUTHENTICATION_MICROSOFT: _ClassVar[BotAuthentication]
BOT_AUTHENTICATION_UNSPECIFIED: BotAuthentication
BOT_AUTHENTICATION_OFFLINE: BotAuthentication
BOT_AUTHENTICATION_MICROSOFT: BotAuthentication

class InstanceUser(_message.Message):
    __slots__ = ("id", "username", "email")
    ID_FIELD_NUMBER: _ClassVar[int]
    USERNAME_FIELD_NUMBER: _ClassVar[int]
    EMAIL_FIELD_NUMBER: _ClassVar[int]
    id: str
    username: str
    email: str
    def __init__(self, id: _Optional[str] = ..., username: _Optional[str] = ..., email: _Optional[str] = ...) -> None: ...

class InstanceConfig(_message.Message):
    __slots__ = ("settings", "accounts", "proxies", "persistent_metadata")
    SETTINGS_FIELD_NUMBER: _ClassVar[int]
    ACCOUNTS_FIELD_NUMBER: _ClassVar[int]
    PROXIES_FIELD_NUMBER: _ClassVar[int]
    PERSISTENT_METADATA_FIELD_NUMBER: _ClassVar[int]
    settings: _containers.RepeatedCompositeFieldContainer[_common_pb2.SettingsNamespace]
    accounts: _containers.RepeatedCompositeFieldContainer[_common_pb2.MinecraftAccountProto]
    proxies: _containers.RepeatedCompositeFieldContainer[_common_pb2.ProxyProto]
    persistent_metadata: _containers.RepeatedCompositeFieldContainer[_common_pb2.SettingsNamespace]
    def __init__(self, settings: _Optional[_Iterable[_Union[_common_pb2.SettingsNamespace, _Mapping]]] = ..., accounts: _Optional[_Iterable[_Union[_common_pb2.MinecraftAccountProto, _Mapping]]] = ..., proxies: _Optional[_Iterable[_Union[_common_pb2.ProxyProto, _Mapping]]] = ..., persistent_metadata: _Optional[_Iterable[_Union[_common_pb2.SettingsNamespace, _Mapping]]] = ...) -> None: ...

class InstanceCreateRequest(_message.Message):
    __slots__ = ("friendlyName",)
    FRIENDLYNAME_FIELD_NUMBER: _ClassVar[int]
    friendlyName: str
    def __init__(self, friendlyName: _Optional[str] = ...) -> None: ...

class InstanceCreateResponse(_message.Message):
    __slots__ = ("id",)
    ID_FIELD_NUMBER: _ClassVar[int]
    id: str
    def __init__(self, id: _Optional[str] = ...) -> None: ...

class InstanceDuplicateRequest(_message.Message):
    __slots__ = ("id", "friendly_name")
    ID_FIELD_NUMBER: _ClassVar[int]
    FRIENDLY_NAME_FIELD_NUMBER: _ClassVar[int]
    id: str
    friendly_name: str
    def __init__(self, id: _Optional[str] = ..., friendly_name: _Optional[str] = ...) -> None: ...

class InstanceDuplicateResponse(_message.Message):
    __slots__ = ("id",)
    ID_FIELD_NUMBER: _ClassVar[int]
    id: str
    def __init__(self, id: _Optional[str] = ...) -> None: ...

class InstanceGetOrCreateRequest(_message.Message):
    __slots__ = ("name", "server")
    NAME_FIELD_NUMBER: _ClassVar[int]
    SERVER_FIELD_NUMBER: _ClassVar[int]
    name: str
    server: str
    def __init__(self, name: _Optional[str] = ..., server: _Optional[str] = ...) -> None: ...

class InstanceGetOrCreateResponse(_message.Message):
    __slots__ = ("id", "created")
    ID_FIELD_NUMBER: _ClassVar[int]
    CREATED_FIELD_NUMBER: _ClassVar[int]
    id: str
    created: bool
    def __init__(self, id: _Optional[str] = ..., created: bool = ...) -> None: ...

class InstanceGetOrCreateBotRequest(_message.Message):
    __slots__ = ("id", "name", "auth", "username", "account")
    ID_FIELD_NUMBER: _ClassVar[int]
    NAME_FIELD_NUMBER: _ClassVar[int]
    AUTH_FIELD_NUMBER: _ClassVar[int]
    USERNAME_FIELD_NUMBER: _ClassVar[int]
    ACCOUNT_FIELD_NUMBER: _ClassVar[int]
    id: str
    name: str
    auth: BotAuthentication
    username: str
    account: _common_pb2.MinecraftAccountProto
    def __init__(self, id: _Optional[str] = ..., name: _Optional[str] = ..., auth: _Optional[_Union[BotAuthentication, str]] = ..., username: _Optional[str] = ..., account: _Optional[_Union[_common_pb2.MinecraftAccountProto, _Mapping]] = ...) -> None: ...

class InstanceGetOrCreateBotResponse(_message.Message):
    __slots__ = ("bot_id", "created")
    BOT_ID_FIELD_NUMBER: _ClassVar[int]
    CREATED_FIELD_NUMBER: _ClassVar[int]
    bot_id: str
    created: bool
    def __init__(self, bot_id: _Optional[str] = ..., created: bool = ...) -> None: ...

class InstanceDeleteRequest(_message.Message):
    __slots__ = ("id",)
    ID_FIELD_NUMBER: _ClassVar[int]
    id: str
    def __init__(self, id: _Optional[str] = ...) -> None: ...

class InstanceDeleteResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceListRequest(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceListResponse(_message.Message):
    __slots__ = ("instances",)
    class Instance(_message.Message):
        __slots__ = ("id", "friendly_name", "icon", "instance_permissions", "bot_summary")
        ID_FIELD_NUMBER: _ClassVar[int]
        FRIENDLY_NAME_FIELD_NUMBER: _ClassVar[int]
        ICON_FIELD_NUMBER: _ClassVar[int]
        INSTANCE_PERMISSIONS_FIELD_NUMBER: _ClassVar[int]
        BOT_SUMMARY_FIELD_NUMBER: _ClassVar[int]
        id: str
        friendly_name: str
        icon: str
        instance_permissions: _containers.RepeatedCompositeFieldContainer[InstancePermissionState]
        bot_summary: _bot_pb2.BotFleetSummary
        def __init__(self, id: _Optional[str] = ..., friendly_name: _Optional[str] = ..., icon: _Optional[str] = ..., instance_permissions: _Optional[_Iterable[_Union[InstancePermissionState, _Mapping]]] = ..., bot_summary: _Optional[_Union[_bot_pb2.BotFleetSummary, _Mapping]] = ...) -> None: ...
    INSTANCES_FIELD_NUMBER: _ClassVar[int]
    instances: _containers.RepeatedCompositeFieldContainer[InstanceListResponse.Instance]
    def __init__(self, instances: _Optional[_Iterable[_Union[InstanceListResponse.Instance, _Mapping]]] = ...) -> None: ...

class InstancePermissionState(_message.Message):
    __slots__ = ("instance_permission", "granted")
    INSTANCE_PERMISSION_FIELD_NUMBER: _ClassVar[int]
    GRANTED_FIELD_NUMBER: _ClassVar[int]
    instance_permission: _common_pb2.InstancePermission
    granted: bool
    def __init__(self, instance_permission: _Optional[_Union[_common_pb2.InstancePermission, str]] = ..., granted: bool = ...) -> None: ...

class InstanceInfoRequest(_message.Message):
    __slots__ = ("id", "if_modified_since")
    ID_FIELD_NUMBER: _ClassVar[int]
    IF_MODIFIED_SINCE_FIELD_NUMBER: _ClassVar[int]
    id: str
    if_modified_since: _timestamp_pb2.Timestamp
    def __init__(self, id: _Optional[str] = ..., if_modified_since: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class InstanceInfo(_message.Message):
    __slots__ = ("friendly_name", "icon", "config", "instance_permissions", "settings_definitions", "instance_settings", "plugins", "last_modified", "bot_summary")
    FRIENDLY_NAME_FIELD_NUMBER: _ClassVar[int]
    ICON_FIELD_NUMBER: _ClassVar[int]
    CONFIG_FIELD_NUMBER: _ClassVar[int]
    INSTANCE_PERMISSIONS_FIELD_NUMBER: _ClassVar[int]
    SETTINGS_DEFINITIONS_FIELD_NUMBER: _ClassVar[int]
    INSTANCE_SETTINGS_FIELD_NUMBER: _ClassVar[int]
    PLUGINS_FIELD_NUMBER: _ClassVar[int]
    LAST_MODIFIED_FIELD_NUMBER: _ClassVar[int]
    BOT_SUMMARY_FIELD_NUMBER: _ClassVar[int]
    friendly_name: str
    icon: str
    config: InstanceConfig
    instance_permissions: _containers.RepeatedCompositeFieldContainer[InstancePermissionState]
    settings_definitions: _containers.RepeatedCompositeFieldContainer[_common_pb2.SettingsDefinition]
    instance_settings: _containers.RepeatedCompositeFieldContainer[_common_pb2.SettingsPage]
    plugins: _containers.RepeatedCompositeFieldContainer[_common_pb2.ServerPlugin]
    last_modified: _timestamp_pb2.Timestamp
    bot_summary: _bot_pb2.BotFleetSummary
    def __init__(self, friendly_name: _Optional[str] = ..., icon: _Optional[str] = ..., config: _Optional[_Union[InstanceConfig, _Mapping]] = ..., instance_permissions: _Optional[_Iterable[_Union[InstancePermissionState, _Mapping]]] = ..., settings_definitions: _Optional[_Iterable[_Union[_common_pb2.SettingsDefinition, _Mapping]]] = ..., instance_settings: _Optional[_Iterable[_Union[_common_pb2.SettingsPage, _Mapping]]] = ..., plugins: _Optional[_Iterable[_Union[_common_pb2.ServerPlugin, _Mapping]]] = ..., last_modified: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., bot_summary: _Optional[_Union[_bot_pb2.BotFleetSummary, _Mapping]] = ...) -> None: ...

class InstanceNotModified(_message.Message):
    __slots__ = ("last_modified",)
    LAST_MODIFIED_FIELD_NUMBER: _ClassVar[int]
    last_modified: _timestamp_pb2.Timestamp
    def __init__(self, last_modified: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class InstanceInfoResponse(_message.Message):
    __slots__ = ("info", "not_modified")
    INFO_FIELD_NUMBER: _ClassVar[int]
    NOT_MODIFIED_FIELD_NUMBER: _ClassVar[int]
    info: InstanceInfo
    not_modified: InstanceNotModified
    def __init__(self, info: _Optional[_Union[InstanceInfo, _Mapping]] = ..., not_modified: _Optional[_Union[InstanceNotModified, _Mapping]] = ...) -> None: ...

class InstanceUpdateMetaRequest(_message.Message):
    __slots__ = ("id", "friendly_name", "icon")
    ID_FIELD_NUMBER: _ClassVar[int]
    FRIENDLY_NAME_FIELD_NUMBER: _ClassVar[int]
    ICON_FIELD_NUMBER: _ClassVar[int]
    id: str
    friendly_name: str
    icon: str
    def __init__(self, id: _Optional[str] = ..., friendly_name: _Optional[str] = ..., icon: _Optional[str] = ...) -> None: ...

class InstanceUpdateMetaResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceUpdateConfigRequest(_message.Message):
    __slots__ = ("id", "config")
    ID_FIELD_NUMBER: _ClassVar[int]
    CONFIG_FIELD_NUMBER: _ClassVar[int]
    id: str
    config: InstanceConfig
    def __init__(self, id: _Optional[str] = ..., config: _Optional[_Union[InstanceConfig, _Mapping]] = ...) -> None: ...

class InstanceUpdateConfigResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceUpdateConfigEntryRequest(_message.Message):
    __slots__ = ("id", "namespace", "key", "value")
    ID_FIELD_NUMBER: _ClassVar[int]
    NAMESPACE_FIELD_NUMBER: _ClassVar[int]
    KEY_FIELD_NUMBER: _ClassVar[int]
    VALUE_FIELD_NUMBER: _ClassVar[int]
    id: str
    namespace: str
    key: str
    value: _struct_pb2.Value
    def __init__(self, id: _Optional[str] = ..., namespace: _Optional[str] = ..., key: _Optional[str] = ..., value: _Optional[_Union[_struct_pb2.Value, _Mapping]] = ...) -> None: ...

class InstanceUpdateConfigEntryResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceAddAccountRequest(_message.Message):
    __slots__ = ("id", "account")
    ID_FIELD_NUMBER: _ClassVar[int]
    ACCOUNT_FIELD_NUMBER: _ClassVar[int]
    id: str
    account: _common_pb2.MinecraftAccountProto
    def __init__(self, id: _Optional[str] = ..., account: _Optional[_Union[_common_pb2.MinecraftAccountProto, _Mapping]] = ...) -> None: ...

class InstanceAddAccountResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceRemoveAccountRequest(_message.Message):
    __slots__ = ("id", "profile_id")
    ID_FIELD_NUMBER: _ClassVar[int]
    PROFILE_ID_FIELD_NUMBER: _ClassVar[int]
    id: str
    profile_id: str
    def __init__(self, id: _Optional[str] = ..., profile_id: _Optional[str] = ...) -> None: ...

class InstanceRemoveAccountResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceUpdateAccountRequest(_message.Message):
    __slots__ = ("id", "account")
    ID_FIELD_NUMBER: _ClassVar[int]
    ACCOUNT_FIELD_NUMBER: _ClassVar[int]
    id: str
    account: _common_pb2.MinecraftAccountProto
    def __init__(self, id: _Optional[str] = ..., account: _Optional[_Union[_common_pb2.MinecraftAccountProto, _Mapping]] = ...) -> None: ...

class InstanceUpdateAccountResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceAddAccountsBatchRequest(_message.Message):
    __slots__ = ("id", "accounts")
    ID_FIELD_NUMBER: _ClassVar[int]
    ACCOUNTS_FIELD_NUMBER: _ClassVar[int]
    id: str
    accounts: _containers.RepeatedCompositeFieldContainer[_common_pb2.MinecraftAccountProto]
    def __init__(self, id: _Optional[str] = ..., accounts: _Optional[_Iterable[_Union[_common_pb2.MinecraftAccountProto, _Mapping]]] = ...) -> None: ...

class InstanceAddAccountsBatchResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceReplaceAccountsRequest(_message.Message):
    __slots__ = ("id", "accounts")
    ID_FIELD_NUMBER: _ClassVar[int]
    ACCOUNTS_FIELD_NUMBER: _ClassVar[int]
    id: str
    accounts: _containers.RepeatedCompositeFieldContainer[_common_pb2.MinecraftAccountProto]
    def __init__(self, id: _Optional[str] = ..., accounts: _Optional[_Iterable[_Union[_common_pb2.MinecraftAccountProto, _Mapping]]] = ...) -> None: ...

class InstanceReplaceAccountsResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceRemoveAccountsBatchRequest(_message.Message):
    __slots__ = ("id", "profile_ids")
    ID_FIELD_NUMBER: _ClassVar[int]
    PROFILE_IDS_FIELD_NUMBER: _ClassVar[int]
    id: str
    profile_ids: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, id: _Optional[str] = ..., profile_ids: _Optional[_Iterable[str]] = ...) -> None: ...

class InstanceRemoveAccountsBatchResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceAddProxyRequest(_message.Message):
    __slots__ = ("id", "proxy")
    ID_FIELD_NUMBER: _ClassVar[int]
    PROXY_FIELD_NUMBER: _ClassVar[int]
    id: str
    proxy: _common_pb2.ProxyProto
    def __init__(self, id: _Optional[str] = ..., proxy: _Optional[_Union[_common_pb2.ProxyProto, _Mapping]] = ...) -> None: ...

class InstanceAddProxyResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceRemoveProxyRequest(_message.Message):
    __slots__ = ("id", "index")
    ID_FIELD_NUMBER: _ClassVar[int]
    INDEX_FIELD_NUMBER: _ClassVar[int]
    id: str
    index: int
    def __init__(self, id: _Optional[str] = ..., index: _Optional[int] = ...) -> None: ...

class InstanceRemoveProxyResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceUpdateProxyRequest(_message.Message):
    __slots__ = ("id", "index", "proxy")
    ID_FIELD_NUMBER: _ClassVar[int]
    INDEX_FIELD_NUMBER: _ClassVar[int]
    PROXY_FIELD_NUMBER: _ClassVar[int]
    id: str
    index: int
    proxy: _common_pb2.ProxyProto
    def __init__(self, id: _Optional[str] = ..., index: _Optional[int] = ..., proxy: _Optional[_Union[_common_pb2.ProxyProto, _Mapping]] = ...) -> None: ...

class InstanceUpdateProxyResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceAddProxiesBatchRequest(_message.Message):
    __slots__ = ("id", "proxies")
    ID_FIELD_NUMBER: _ClassVar[int]
    PROXIES_FIELD_NUMBER: _ClassVar[int]
    id: str
    proxies: _containers.RepeatedCompositeFieldContainer[_common_pb2.ProxyProto]
    def __init__(self, id: _Optional[str] = ..., proxies: _Optional[_Iterable[_Union[_common_pb2.ProxyProto, _Mapping]]] = ...) -> None: ...

class InstanceAddProxiesBatchResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceRemoveProxiesBatchRequest(_message.Message):
    __slots__ = ("id", "addresses")
    ID_FIELD_NUMBER: _ClassVar[int]
    ADDRESSES_FIELD_NUMBER: _ClassVar[int]
    id: str
    addresses: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, id: _Optional[str] = ..., addresses: _Optional[_Iterable[str]] = ...) -> None: ...

class InstanceRemoveProxiesBatchResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceRemoveProxyEntriesRequest(_message.Message):
    __slots__ = ("id", "proxies")
    ID_FIELD_NUMBER: _ClassVar[int]
    PROXIES_FIELD_NUMBER: _ClassVar[int]
    id: str
    proxies: _containers.RepeatedCompositeFieldContainer[_common_pb2.ProxyProto]
    def __init__(self, id: _Optional[str] = ..., proxies: _Optional[_Iterable[_Union[_common_pb2.ProxyProto, _Mapping]]] = ...) -> None: ...

class InstanceRemoveProxyEntriesResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceAuditLogRequest(_message.Message):
    __slots__ = ("id",)
    ID_FIELD_NUMBER: _ClassVar[int]
    id: str
    def __init__(self, id: _Optional[str] = ...) -> None: ...

class GetAccountMetadataRequest(_message.Message):
    __slots__ = ("instance_id", "account_id")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    ACCOUNT_ID_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    account_id: str
    def __init__(self, instance_id: _Optional[str] = ..., account_id: _Optional[str] = ...) -> None: ...

class GetAccountMetadataResponse(_message.Message):
    __slots__ = ("metadata",)
    METADATA_FIELD_NUMBER: _ClassVar[int]
    metadata: _containers.RepeatedCompositeFieldContainer[_common_pb2.SettingsNamespace]
    def __init__(self, metadata: _Optional[_Iterable[_Union[_common_pb2.SettingsNamespace, _Mapping]]] = ...) -> None: ...

class SetAccountMetadataEntryRequest(_message.Message):
    __slots__ = ("instance_id", "account_id", "namespace", "key", "value")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    ACCOUNT_ID_FIELD_NUMBER: _ClassVar[int]
    NAMESPACE_FIELD_NUMBER: _ClassVar[int]
    KEY_FIELD_NUMBER: _ClassVar[int]
    VALUE_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    account_id: str
    namespace: str
    key: str
    value: _struct_pb2.Value
    def __init__(self, instance_id: _Optional[str] = ..., account_id: _Optional[str] = ..., namespace: _Optional[str] = ..., key: _Optional[str] = ..., value: _Optional[_Union[_struct_pb2.Value, _Mapping]] = ...) -> None: ...

class SetAccountMetadataEntryResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class DeleteAccountMetadataEntryRequest(_message.Message):
    __slots__ = ("instance_id", "account_id", "namespace", "key")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    ACCOUNT_ID_FIELD_NUMBER: _ClassVar[int]
    NAMESPACE_FIELD_NUMBER: _ClassVar[int]
    KEY_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    account_id: str
    namespace: str
    key: str
    def __init__(self, instance_id: _Optional[str] = ..., account_id: _Optional[str] = ..., namespace: _Optional[str] = ..., key: _Optional[str] = ...) -> None: ...

class DeleteAccountMetadataEntryResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class GetInstanceMetadataRequest(_message.Message):
    __slots__ = ("instance_id",)
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    def __init__(self, instance_id: _Optional[str] = ...) -> None: ...

class GetInstanceMetadataResponse(_message.Message):
    __slots__ = ("metadata",)
    METADATA_FIELD_NUMBER: _ClassVar[int]
    metadata: _containers.RepeatedCompositeFieldContainer[_common_pb2.SettingsNamespace]
    def __init__(self, metadata: _Optional[_Iterable[_Union[_common_pb2.SettingsNamespace, _Mapping]]] = ...) -> None: ...

class SetInstanceMetadataEntryRequest(_message.Message):
    __slots__ = ("instance_id", "namespace", "key", "value")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    NAMESPACE_FIELD_NUMBER: _ClassVar[int]
    KEY_FIELD_NUMBER: _ClassVar[int]
    VALUE_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    namespace: str
    key: str
    value: _struct_pb2.Value
    def __init__(self, instance_id: _Optional[str] = ..., namespace: _Optional[str] = ..., key: _Optional[str] = ..., value: _Optional[_Union[_struct_pb2.Value, _Mapping]] = ...) -> None: ...

class SetInstanceMetadataEntryResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class DeleteInstanceMetadataEntryRequest(_message.Message):
    __slots__ = ("instance_id", "namespace", "key")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    NAMESPACE_FIELD_NUMBER: _ClassVar[int]
    KEY_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    namespace: str
    key: str
    def __init__(self, instance_id: _Optional[str] = ..., namespace: _Optional[str] = ..., key: _Optional[str] = ...) -> None: ...

class DeleteInstanceMetadataEntryResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class InstanceAuditLogResponse(_message.Message):
    __slots__ = ("entry",)
    class AuditLogEntryType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
        __slots__ = ()
        EXECUTE_COMMAND: _ClassVar[InstanceAuditLogResponse.AuditLogEntryType]
        BOT_DESIRED_STATE_CHANGE: _ClassVar[InstanceAuditLogResponse.AuditLogEntryType]
        BOT_RESTART: _ClassVar[InstanceAuditLogResponse.AuditLogEntryType]
        PLUGIN_RPC: _ClassVar[InstanceAuditLogResponse.AuditLogEntryType]
    EXECUTE_COMMAND: InstanceAuditLogResponse.AuditLogEntryType
    BOT_DESIRED_STATE_CHANGE: InstanceAuditLogResponse.AuditLogEntryType
    BOT_RESTART: InstanceAuditLogResponse.AuditLogEntryType
    PLUGIN_RPC: InstanceAuditLogResponse.AuditLogEntryType
    class AuditLogEntry(_message.Message):
        __slots__ = ("id", "user", "type", "timestamp", "data")
        ID_FIELD_NUMBER: _ClassVar[int]
        USER_FIELD_NUMBER: _ClassVar[int]
        TYPE_FIELD_NUMBER: _ClassVar[int]
        TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
        DATA_FIELD_NUMBER: _ClassVar[int]
        id: str
        user: InstanceUser
        type: InstanceAuditLogResponse.AuditLogEntryType
        timestamp: _timestamp_pb2.Timestamp
        data: str
        def __init__(self, id: _Optional[str] = ..., user: _Optional[_Union[InstanceUser, _Mapping]] = ..., type: _Optional[_Union[InstanceAuditLogResponse.AuditLogEntryType, str]] = ..., timestamp: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., data: _Optional[str] = ...) -> None: ...
    ENTRY_FIELD_NUMBER: _ClassVar[int]
    entry: _containers.RepeatedCompositeFieldContainer[InstanceAuditLogResponse.AuditLogEntry]
    def __init__(self, entry: _Optional[_Iterable[_Union[InstanceAuditLogResponse.AuditLogEntry, _Mapping]]] = ...) -> None: ...
