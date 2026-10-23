import datetime

from soulfire import api_docs_pb2 as _api_docs_pb2
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

class EdgeType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    EDGE_TYPE_EXECUTION: _ClassVar[EdgeType]
    EDGE_TYPE_DATA: _ClassVar[EdgeType]

class PortType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    PORT_TYPE_ANY: _ClassVar[PortType]
    PORT_TYPE_NUMBER: _ClassVar[PortType]
    PORT_TYPE_STRING: _ClassVar[PortType]
    PORT_TYPE_BOOLEAN: _ClassVar[PortType]
    PORT_TYPE_VECTOR3: _ClassVar[PortType]
    PORT_TYPE_BOT: _ClassVar[PortType]
    PORT_TYPE_LIST: _ClassVar[PortType]
    PORT_TYPE_EXEC: _ClassVar[PortType]
    PORT_TYPE_BLOCK: _ClassVar[PortType]
    PORT_TYPE_ENTITY: _ClassVar[PortType]
    PORT_TYPE_ITEM: _ClassVar[PortType]
    PORT_TYPE_MAP: _ClassVar[PortType]
    PORT_TYPE_SET: _ClassVar[PortType]
    PORT_TYPE_COLLECTION: _ClassVar[PortType]

class LogLevel(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    LOG_LEVEL_DEBUG: _ClassVar[LogLevel]
    LOG_LEVEL_INFO: _ClassVar[LogLevel]
    LOG_LEVEL_WARN: _ClassVar[LogLevel]
    LOG_LEVEL_ERROR: _ClassVar[LogLevel]

class DiagnosticSeverity(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    DIAGNOSTIC_ERROR: _ClassVar[DiagnosticSeverity]
    DIAGNOSTIC_WARNING: _ClassVar[DiagnosticSeverity]

class HandleShape(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    HANDLE_SHAPE_CIRCLE: _ClassVar[HandleShape]
    HANDLE_SHAPE_SQUARE: _ClassVar[HandleShape]
    HANDLE_SHAPE_DIAMOND: _ClassVar[HandleShape]

class EdgeStyle(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    EDGE_STYLE_DEFAULT: _ClassVar[EdgeStyle]
    EDGE_STYLE_ANIMATED: _ClassVar[EdgeStyle]
    EDGE_STYLE_DASHED: _ClassVar[EdgeStyle]
EDGE_TYPE_EXECUTION: EdgeType
EDGE_TYPE_DATA: EdgeType
PORT_TYPE_ANY: PortType
PORT_TYPE_NUMBER: PortType
PORT_TYPE_STRING: PortType
PORT_TYPE_BOOLEAN: PortType
PORT_TYPE_VECTOR3: PortType
PORT_TYPE_BOT: PortType
PORT_TYPE_LIST: PortType
PORT_TYPE_EXEC: PortType
PORT_TYPE_BLOCK: PortType
PORT_TYPE_ENTITY: PortType
PORT_TYPE_ITEM: PortType
PORT_TYPE_MAP: PortType
PORT_TYPE_SET: PortType
PORT_TYPE_COLLECTION: PortType
LOG_LEVEL_DEBUG: LogLevel
LOG_LEVEL_INFO: LogLevel
LOG_LEVEL_WARN: LogLevel
LOG_LEVEL_ERROR: LogLevel
DIAGNOSTIC_ERROR: DiagnosticSeverity
DIAGNOSTIC_WARNING: DiagnosticSeverity
HANDLE_SHAPE_CIRCLE: HandleShape
HANDLE_SHAPE_SQUARE: HandleShape
HANDLE_SHAPE_DIAMOND: HandleShape
EDGE_STYLE_DEFAULT: EdgeStyle
EDGE_STYLE_ANIMATED: EdgeStyle
EDGE_STYLE_DASHED: EdgeStyle

class Position(_message.Message):
    __slots__ = ("x", "y")
    X_FIELD_NUMBER: _ClassVar[int]
    Y_FIELD_NUMBER: _ClassVar[int]
    x: float
    y: float
    def __init__(self, x: _Optional[float] = ..., y: _Optional[float] = ...) -> None: ...

class TypeDescriptor(_message.Message):
    __slots__ = ("simple", "parameterized", "type_variable")
    SIMPLE_FIELD_NUMBER: _ClassVar[int]
    PARAMETERIZED_FIELD_NUMBER: _ClassVar[int]
    TYPE_VARIABLE_FIELD_NUMBER: _ClassVar[int]
    simple: PortType
    parameterized: ParameterizedType
    type_variable: str
    def __init__(self, simple: _Optional[_Union[PortType, str]] = ..., parameterized: _Optional[_Union[ParameterizedType, _Mapping]] = ..., type_variable: _Optional[str] = ...) -> None: ...

class ParameterizedType(_message.Message):
    __slots__ = ("base", "params")
    BASE_FIELD_NUMBER: _ClassVar[int]
    PARAMS_FIELD_NUMBER: _ClassVar[int]
    base: PortType
    params: _containers.RepeatedCompositeFieldContainer[TypeDescriptor]
    def __init__(self, base: _Optional[_Union[PortType, str]] = ..., params: _Optional[_Iterable[_Union[TypeDescriptor, _Mapping]]] = ...) -> None: ...

class PortDefinition(_message.Message):
    __slots__ = ("id", "display_name", "port_type", "required", "default_value", "description", "element_type", "multi_input", "accepted_types", "infer_type_from", "type_descriptor")
    ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    PORT_TYPE_FIELD_NUMBER: _ClassVar[int]
    REQUIRED_FIELD_NUMBER: _ClassVar[int]
    DEFAULT_VALUE_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    ELEMENT_TYPE_FIELD_NUMBER: _ClassVar[int]
    MULTI_INPUT_FIELD_NUMBER: _ClassVar[int]
    ACCEPTED_TYPES_FIELD_NUMBER: _ClassVar[int]
    INFER_TYPE_FROM_FIELD_NUMBER: _ClassVar[int]
    TYPE_DESCRIPTOR_FIELD_NUMBER: _ClassVar[int]
    id: str
    display_name: str
    port_type: PortType
    required: bool
    default_value: str
    description: str
    element_type: PortType
    multi_input: bool
    accepted_types: _containers.RepeatedScalarFieldContainer[PortType]
    infer_type_from: str
    type_descriptor: TypeDescriptor
    def __init__(self, id: _Optional[str] = ..., display_name: _Optional[str] = ..., port_type: _Optional[_Union[PortType, str]] = ..., required: bool = ..., default_value: _Optional[str] = ..., description: _Optional[str] = ..., element_type: _Optional[_Union[PortType, str]] = ..., multi_input: bool = ..., accepted_types: _Optional[_Iterable[_Union[PortType, str]]] = ..., infer_type_from: _Optional[str] = ..., type_descriptor: _Optional[_Union[TypeDescriptor, _Mapping]] = ...) -> None: ...

class NodeTypeDefinition(_message.Message):
    __slots__ = ("type", "display_name", "description", "category", "is_trigger", "inputs", "outputs", "icon", "color", "keywords", "deprecated", "deprecation_message", "is_layout_node", "supports_muting", "supports_preview", "is_expensive")
    TYPE_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    CATEGORY_FIELD_NUMBER: _ClassVar[int]
    IS_TRIGGER_FIELD_NUMBER: _ClassVar[int]
    INPUTS_FIELD_NUMBER: _ClassVar[int]
    OUTPUTS_FIELD_NUMBER: _ClassVar[int]
    ICON_FIELD_NUMBER: _ClassVar[int]
    COLOR_FIELD_NUMBER: _ClassVar[int]
    KEYWORDS_FIELD_NUMBER: _ClassVar[int]
    DEPRECATED_FIELD_NUMBER: _ClassVar[int]
    DEPRECATION_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    IS_LAYOUT_NODE_FIELD_NUMBER: _ClassVar[int]
    SUPPORTS_MUTING_FIELD_NUMBER: _ClassVar[int]
    SUPPORTS_PREVIEW_FIELD_NUMBER: _ClassVar[int]
    IS_EXPENSIVE_FIELD_NUMBER: _ClassVar[int]
    type: str
    display_name: str
    description: str
    category: str
    is_trigger: bool
    inputs: _containers.RepeatedCompositeFieldContainer[PortDefinition]
    outputs: _containers.RepeatedCompositeFieldContainer[PortDefinition]
    icon: str
    color: str
    keywords: _containers.RepeatedScalarFieldContainer[str]
    deprecated: bool
    deprecation_message: str
    is_layout_node: bool
    supports_muting: bool
    supports_preview: bool
    is_expensive: bool
    def __init__(self, type: _Optional[str] = ..., display_name: _Optional[str] = ..., description: _Optional[str] = ..., category: _Optional[str] = ..., is_trigger: bool = ..., inputs: _Optional[_Iterable[_Union[PortDefinition, _Mapping]]] = ..., outputs: _Optional[_Iterable[_Union[PortDefinition, _Mapping]]] = ..., icon: _Optional[str] = ..., color: _Optional[str] = ..., keywords: _Optional[_Iterable[str]] = ..., deprecated: bool = ..., deprecation_message: _Optional[str] = ..., is_layout_node: bool = ..., supports_muting: bool = ..., supports_preview: bool = ..., is_expensive: bool = ...) -> None: ...

class ScriptNode(_message.Message):
    __slots__ = ("id", "type", "position", "data", "muted", "collapsed", "width", "height", "contained_nodes", "label", "resolved_type", "parent_frame_id")
    class DataEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: _struct_pb2.Value
        def __init__(self, key: _Optional[str] = ..., value: _Optional[_Union[_struct_pb2.Value, _Mapping]] = ...) -> None: ...
    ID_FIELD_NUMBER: _ClassVar[int]
    TYPE_FIELD_NUMBER: _ClassVar[int]
    POSITION_FIELD_NUMBER: _ClassVar[int]
    DATA_FIELD_NUMBER: _ClassVar[int]
    MUTED_FIELD_NUMBER: _ClassVar[int]
    COLLAPSED_FIELD_NUMBER: _ClassVar[int]
    WIDTH_FIELD_NUMBER: _ClassVar[int]
    HEIGHT_FIELD_NUMBER: _ClassVar[int]
    CONTAINED_NODES_FIELD_NUMBER: _ClassVar[int]
    LABEL_FIELD_NUMBER: _ClassVar[int]
    RESOLVED_TYPE_FIELD_NUMBER: _ClassVar[int]
    PARENT_FRAME_ID_FIELD_NUMBER: _ClassVar[int]
    id: str
    type: str
    position: Position
    data: _containers.MessageMap[str, _struct_pb2.Value]
    muted: bool
    collapsed: bool
    width: float
    height: float
    contained_nodes: _containers.RepeatedScalarFieldContainer[str]
    label: str
    resolved_type: PortType
    parent_frame_id: str
    def __init__(self, id: _Optional[str] = ..., type: _Optional[str] = ..., position: _Optional[_Union[Position, _Mapping]] = ..., data: _Optional[_Mapping[str, _struct_pb2.Value]] = ..., muted: bool = ..., collapsed: bool = ..., width: _Optional[float] = ..., height: _Optional[float] = ..., contained_nodes: _Optional[_Iterable[str]] = ..., label: _Optional[str] = ..., resolved_type: _Optional[_Union[PortType, str]] = ..., parent_frame_id: _Optional[str] = ...) -> None: ...

class ScriptEdge(_message.Message):
    __slots__ = ("id", "source", "source_handle", "target", "target_handle", "edge_type", "order")
    ID_FIELD_NUMBER: _ClassVar[int]
    SOURCE_FIELD_NUMBER: _ClassVar[int]
    SOURCE_HANDLE_FIELD_NUMBER: _ClassVar[int]
    TARGET_FIELD_NUMBER: _ClassVar[int]
    TARGET_HANDLE_FIELD_NUMBER: _ClassVar[int]
    EDGE_TYPE_FIELD_NUMBER: _ClassVar[int]
    ORDER_FIELD_NUMBER: _ClassVar[int]
    id: str
    source: str
    source_handle: str
    target: str
    target_handle: str
    edge_type: EdgeType
    order: int
    def __init__(self, id: _Optional[str] = ..., source: _Optional[str] = ..., source_handle: _Optional[str] = ..., target: _Optional[str] = ..., target_handle: _Optional[str] = ..., edge_type: _Optional[_Union[EdgeType, str]] = ..., order: _Optional[int] = ...) -> None: ...

class ScriptData(_message.Message):
    __slots__ = ("id", "name", "description", "nodes", "edges", "instance_id", "paused", "quotas")
    ID_FIELD_NUMBER: _ClassVar[int]
    NAME_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    NODES_FIELD_NUMBER: _ClassVar[int]
    EDGES_FIELD_NUMBER: _ClassVar[int]
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    PAUSED_FIELD_NUMBER: _ClassVar[int]
    QUOTAS_FIELD_NUMBER: _ClassVar[int]
    id: str
    name: str
    description: str
    nodes: _containers.RepeatedCompositeFieldContainer[ScriptNode]
    edges: _containers.RepeatedCompositeFieldContainer[ScriptEdge]
    instance_id: str
    paused: bool
    quotas: ScriptQuotas
    def __init__(self, id: _Optional[str] = ..., name: _Optional[str] = ..., description: _Optional[str] = ..., nodes: _Optional[_Iterable[_Union[ScriptNode, _Mapping]]] = ..., edges: _Optional[_Iterable[_Union[ScriptEdge, _Mapping]]] = ..., instance_id: _Optional[str] = ..., paused: bool = ..., quotas: _Optional[_Union[ScriptQuotas, _Mapping]] = ...) -> None: ...

class ScriptQuotas(_message.Message):
    __slots__ = ("max_execution_count", "max_execution_time_ms", "max_concurrent_triggers", "max_state_store_entries", "disable_timeouts")
    MAX_EXECUTION_COUNT_FIELD_NUMBER: _ClassVar[int]
    MAX_EXECUTION_TIME_MS_FIELD_NUMBER: _ClassVar[int]
    MAX_CONCURRENT_TRIGGERS_FIELD_NUMBER: _ClassVar[int]
    MAX_STATE_STORE_ENTRIES_FIELD_NUMBER: _ClassVar[int]
    DISABLE_TIMEOUTS_FIELD_NUMBER: _ClassVar[int]
    max_execution_count: int
    max_execution_time_ms: int
    max_concurrent_triggers: int
    max_state_store_entries: int
    disable_timeouts: bool
    def __init__(self, max_execution_count: _Optional[int] = ..., max_execution_time_ms: _Optional[int] = ..., max_concurrent_triggers: _Optional[int] = ..., max_state_store_entries: _Optional[int] = ..., disable_timeouts: bool = ...) -> None: ...

class ScriptInfo(_message.Message):
    __slots__ = ("id", "name", "description", "instance_id", "created_at", "updated_at", "paused")
    ID_FIELD_NUMBER: _ClassVar[int]
    NAME_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    CREATED_AT_FIELD_NUMBER: _ClassVar[int]
    UPDATED_AT_FIELD_NUMBER: _ClassVar[int]
    PAUSED_FIELD_NUMBER: _ClassVar[int]
    id: str
    name: str
    description: str
    instance_id: str
    created_at: _timestamp_pb2.Timestamp
    updated_at: _timestamp_pb2.Timestamp
    paused: bool
    def __init__(self, id: _Optional[str] = ..., name: _Optional[str] = ..., description: _Optional[str] = ..., instance_id: _Optional[str] = ..., created_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., updated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., paused: bool = ...) -> None: ...

class ScriptStatus(_message.Message):
    __slots__ = ("script_id", "is_active", "active_node_id", "activation_count", "paused")
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    IS_ACTIVE_FIELD_NUMBER: _ClassVar[int]
    ACTIVE_NODE_ID_FIELD_NUMBER: _ClassVar[int]
    ACTIVATION_COUNT_FIELD_NUMBER: _ClassVar[int]
    PAUSED_FIELD_NUMBER: _ClassVar[int]
    script_id: str
    is_active: bool
    active_node_id: str
    activation_count: int
    paused: bool
    def __init__(self, script_id: _Optional[str] = ..., is_active: bool = ..., active_node_id: _Optional[str] = ..., activation_count: _Optional[int] = ..., paused: bool = ...) -> None: ...

class ScriptLogEntry(_message.Message):
    __slots__ = ("script_id", "node_id", "level", "message", "timestamp")
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    LEVEL_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
    script_id: str
    node_id: str
    level: LogLevel
    message: str
    timestamp: _timestamp_pb2.Timestamp
    def __init__(self, script_id: _Optional[str] = ..., node_id: _Optional[str] = ..., level: _Optional[_Union[LogLevel, str]] = ..., message: _Optional[str] = ..., timestamp: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class NodeStarted(_message.Message):
    __slots__ = ("node_id", "timestamp", "execution_id")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
    EXECUTION_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    timestamp: _timestamp_pb2.Timestamp
    execution_id: int
    def __init__(self, node_id: _Optional[str] = ..., timestamp: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., execution_id: _Optional[int] = ...) -> None: ...

class NodeCompleted(_message.Message):
    __slots__ = ("node_id", "outputs", "timestamp", "execution_time_nanos", "execution_id")
    class OutputsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: _struct_pb2.Value
        def __init__(self, key: _Optional[str] = ..., value: _Optional[_Union[_struct_pb2.Value, _Mapping]] = ...) -> None: ...
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    OUTPUTS_FIELD_NUMBER: _ClassVar[int]
    TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
    EXECUTION_TIME_NANOS_FIELD_NUMBER: _ClassVar[int]
    EXECUTION_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    outputs: _containers.MessageMap[str, _struct_pb2.Value]
    timestamp: _timestamp_pb2.Timestamp
    execution_time_nanos: int
    execution_id: int
    def __init__(self, node_id: _Optional[str] = ..., outputs: _Optional[_Mapping[str, _struct_pb2.Value]] = ..., timestamp: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., execution_time_nanos: _Optional[int] = ..., execution_id: _Optional[int] = ...) -> None: ...

class ExecutionStats(_message.Message):
    __slots__ = ("node_count", "max_count")
    NODE_COUNT_FIELD_NUMBER: _ClassVar[int]
    MAX_COUNT_FIELD_NUMBER: _ClassVar[int]
    node_count: int
    max_count: int
    def __init__(self, node_count: _Optional[int] = ..., max_count: _Optional[int] = ...) -> None: ...

class NodeError(_message.Message):
    __slots__ = ("node_id", "error_message", "timestamp", "execution_id")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
    EXECUTION_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    error_message: str
    timestamp: _timestamp_pb2.Timestamp
    execution_id: int
    def __init__(self, node_id: _Optional[str] = ..., error_message: _Optional[str] = ..., timestamp: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., execution_id: _Optional[int] = ...) -> None: ...

class ScriptLog(_message.Message):
    __slots__ = ("node_id", "level", "message", "timestamp")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    LEVEL_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    level: str
    message: str
    timestamp: _timestamp_pb2.Timestamp
    def __init__(self, node_id: _Optional[str] = ..., level: _Optional[str] = ..., message: _Optional[str] = ..., timestamp: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class ScriptStarted(_message.Message):
    __slots__ = ("script_id", "timestamp")
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
    script_id: str
    timestamp: _timestamp_pb2.Timestamp
    def __init__(self, script_id: _Optional[str] = ..., timestamp: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class ScriptCompleted(_message.Message):
    __slots__ = ("script_id", "success", "timestamp")
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
    script_id: str
    success: bool
    timestamp: _timestamp_pb2.Timestamp
    def __init__(self, script_id: _Optional[str] = ..., success: bool = ..., timestamp: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class ValidationDiagnostic(_message.Message):
    __slots__ = ("node_id", "edge_id", "message", "severity")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    EDGE_ID_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    SEVERITY_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    edge_id: str
    message: str
    severity: DiagnosticSeverity
    def __init__(self, node_id: _Optional[str] = ..., edge_id: _Optional[str] = ..., message: _Optional[str] = ..., severity: _Optional[_Union[DiagnosticSeverity, str]] = ...) -> None: ...

class ScriptEvent(_message.Message):
    __slots__ = ("node_started", "node_completed", "node_error", "script_completed", "script_started", "script_log", "execution_stats")
    NODE_STARTED_FIELD_NUMBER: _ClassVar[int]
    NODE_COMPLETED_FIELD_NUMBER: _ClassVar[int]
    NODE_ERROR_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_COMPLETED_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_STARTED_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_LOG_FIELD_NUMBER: _ClassVar[int]
    EXECUTION_STATS_FIELD_NUMBER: _ClassVar[int]
    node_started: NodeStarted
    node_completed: NodeCompleted
    node_error: NodeError
    script_completed: ScriptCompleted
    script_started: ScriptStarted
    script_log: ScriptLog
    execution_stats: ExecutionStats
    def __init__(self, node_started: _Optional[_Union[NodeStarted, _Mapping]] = ..., node_completed: _Optional[_Union[NodeCompleted, _Mapping]] = ..., node_error: _Optional[_Union[NodeError, _Mapping]] = ..., script_completed: _Optional[_Union[ScriptCompleted, _Mapping]] = ..., script_started: _Optional[_Union[ScriptStarted, _Mapping]] = ..., script_log: _Optional[_Union[ScriptLog, _Mapping]] = ..., execution_stats: _Optional[_Union[ExecutionStats, _Mapping]] = ...) -> None: ...

class CreateScriptRequest(_message.Message):
    __slots__ = ("instance_id", "name", "description", "nodes", "edges", "paused", "quotas")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    NAME_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    NODES_FIELD_NUMBER: _ClassVar[int]
    EDGES_FIELD_NUMBER: _ClassVar[int]
    PAUSED_FIELD_NUMBER: _ClassVar[int]
    QUOTAS_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    name: str
    description: str
    nodes: _containers.RepeatedCompositeFieldContainer[ScriptNode]
    edges: _containers.RepeatedCompositeFieldContainer[ScriptEdge]
    paused: bool
    quotas: ScriptQuotas
    def __init__(self, instance_id: _Optional[str] = ..., name: _Optional[str] = ..., description: _Optional[str] = ..., nodes: _Optional[_Iterable[_Union[ScriptNode, _Mapping]]] = ..., edges: _Optional[_Iterable[_Union[ScriptEdge, _Mapping]]] = ..., paused: bool = ..., quotas: _Optional[_Union[ScriptQuotas, _Mapping]] = ...) -> None: ...

class CreateScriptResponse(_message.Message):
    __slots__ = ("script", "diagnostics")
    SCRIPT_FIELD_NUMBER: _ClassVar[int]
    DIAGNOSTICS_FIELD_NUMBER: _ClassVar[int]
    script: ScriptData
    diagnostics: _containers.RepeatedCompositeFieldContainer[ValidationDiagnostic]
    def __init__(self, script: _Optional[_Union[ScriptData, _Mapping]] = ..., diagnostics: _Optional[_Iterable[_Union[ValidationDiagnostic, _Mapping]]] = ...) -> None: ...

class GetScriptRequest(_message.Message):
    __slots__ = ("instance_id", "script_id")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    script_id: str
    def __init__(self, instance_id: _Optional[str] = ..., script_id: _Optional[str] = ...) -> None: ...

class GetScriptResponse(_message.Message):
    __slots__ = ("script",)
    SCRIPT_FIELD_NUMBER: _ClassVar[int]
    script: ScriptData
    def __init__(self, script: _Optional[_Union[ScriptData, _Mapping]] = ...) -> None: ...

class UpdateScriptRequest(_message.Message):
    __slots__ = ("instance_id", "script_id", "name", "description", "nodes", "edges", "update_nodes", "update_edges", "paused", "quotas", "update_quotas")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    NAME_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    NODES_FIELD_NUMBER: _ClassVar[int]
    EDGES_FIELD_NUMBER: _ClassVar[int]
    UPDATE_NODES_FIELD_NUMBER: _ClassVar[int]
    UPDATE_EDGES_FIELD_NUMBER: _ClassVar[int]
    PAUSED_FIELD_NUMBER: _ClassVar[int]
    QUOTAS_FIELD_NUMBER: _ClassVar[int]
    UPDATE_QUOTAS_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    script_id: str
    name: str
    description: str
    nodes: _containers.RepeatedCompositeFieldContainer[ScriptNode]
    edges: _containers.RepeatedCompositeFieldContainer[ScriptEdge]
    update_nodes: bool
    update_edges: bool
    paused: bool
    quotas: ScriptQuotas
    update_quotas: bool
    def __init__(self, instance_id: _Optional[str] = ..., script_id: _Optional[str] = ..., name: _Optional[str] = ..., description: _Optional[str] = ..., nodes: _Optional[_Iterable[_Union[ScriptNode, _Mapping]]] = ..., edges: _Optional[_Iterable[_Union[ScriptEdge, _Mapping]]] = ..., update_nodes: bool = ..., update_edges: bool = ..., paused: bool = ..., quotas: _Optional[_Union[ScriptQuotas, _Mapping]] = ..., update_quotas: bool = ...) -> None: ...

class UpdateScriptResponse(_message.Message):
    __slots__ = ("script", "diagnostics")
    SCRIPT_FIELD_NUMBER: _ClassVar[int]
    DIAGNOSTICS_FIELD_NUMBER: _ClassVar[int]
    script: ScriptData
    diagnostics: _containers.RepeatedCompositeFieldContainer[ValidationDiagnostic]
    def __init__(self, script: _Optional[_Union[ScriptData, _Mapping]] = ..., diagnostics: _Optional[_Iterable[_Union[ValidationDiagnostic, _Mapping]]] = ...) -> None: ...

class DeleteScriptRequest(_message.Message):
    __slots__ = ("instance_id", "script_id")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    script_id: str
    def __init__(self, instance_id: _Optional[str] = ..., script_id: _Optional[str] = ...) -> None: ...

class DeleteScriptResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class ListScriptsRequest(_message.Message):
    __slots__ = ("instance_id",)
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    def __init__(self, instance_id: _Optional[str] = ...) -> None: ...

class ListScriptsResponse(_message.Message):
    __slots__ = ("scripts",)
    SCRIPTS_FIELD_NUMBER: _ClassVar[int]
    scripts: _containers.RepeatedCompositeFieldContainer[ScriptInfo]
    def __init__(self, scripts: _Optional[_Iterable[_Union[ScriptInfo, _Mapping]]] = ...) -> None: ...

class ActivateScriptRequest(_message.Message):
    __slots__ = ("instance_id", "script_id")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    script_id: str
    def __init__(self, instance_id: _Optional[str] = ..., script_id: _Optional[str] = ...) -> None: ...

class DeactivateScriptRequest(_message.Message):
    __slots__ = ("instance_id", "script_id")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    script_id: str
    def __init__(self, instance_id: _Optional[str] = ..., script_id: _Optional[str] = ...) -> None: ...

class DeactivateScriptResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class GetScriptStatusRequest(_message.Message):
    __slots__ = ("instance_id", "script_id")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    script_id: str
    def __init__(self, instance_id: _Optional[str] = ..., script_id: _Optional[str] = ...) -> None: ...

class GetScriptStatusResponse(_message.Message):
    __slots__ = ("status",)
    STATUS_FIELD_NUMBER: _ClassVar[int]
    status: ScriptStatus
    def __init__(self, status: _Optional[_Union[ScriptStatus, _Mapping]] = ...) -> None: ...

class SubscribeScriptLogsRequest(_message.Message):
    __slots__ = ("instance_id", "script_id", "min_level")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    MIN_LEVEL_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    script_id: str
    min_level: LogLevel
    def __init__(self, instance_id: _Optional[str] = ..., script_id: _Optional[str] = ..., min_level: _Optional[_Union[LogLevel, str]] = ...) -> None: ...

class GetNodeTypesRequest(_message.Message):
    __slots__ = ("category", "include_deprecated")
    CATEGORY_FIELD_NUMBER: _ClassVar[int]
    INCLUDE_DEPRECATED_FIELD_NUMBER: _ClassVar[int]
    category: str
    include_deprecated: bool
    def __init__(self, category: _Optional[str] = ..., include_deprecated: bool = ...) -> None: ...

class CategoryDefinition(_message.Message):
    __slots__ = ("id", "display_name", "icon", "description", "sort_order")
    ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    ICON_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    SORT_ORDER_FIELD_NUMBER: _ClassVar[int]
    id: str
    display_name: str
    icon: str
    description: str
    sort_order: int
    def __init__(self, id: _Optional[str] = ..., display_name: _Optional[str] = ..., icon: _Optional[str] = ..., description: _Optional[str] = ..., sort_order: _Optional[int] = ...) -> None: ...

class GetNodeTypesResponse(_message.Message):
    __slots__ = ("node_types", "categories", "port_type_metadata")
    NODE_TYPES_FIELD_NUMBER: _ClassVar[int]
    CATEGORIES_FIELD_NUMBER: _ClassVar[int]
    PORT_TYPE_METADATA_FIELD_NUMBER: _ClassVar[int]
    node_types: _containers.RepeatedCompositeFieldContainer[NodeTypeDefinition]
    categories: _containers.RepeatedCompositeFieldContainer[CategoryDefinition]
    port_type_metadata: _containers.RepeatedCompositeFieldContainer[PortTypeMetadata]
    def __init__(self, node_types: _Optional[_Iterable[_Union[NodeTypeDefinition, _Mapping]]] = ..., categories: _Optional[_Iterable[_Union[CategoryDefinition, _Mapping]]] = ..., port_type_metadata: _Optional[_Iterable[_Union[PortTypeMetadata, _Mapping]]] = ...) -> None: ...

class PortTypeMetadata(_message.Message):
    __slots__ = ("port_type", "color", "display_name", "compatible_from", "handle_shape", "edge_style")
    PORT_TYPE_FIELD_NUMBER: _ClassVar[int]
    COLOR_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    COMPATIBLE_FROM_FIELD_NUMBER: _ClassVar[int]
    HANDLE_SHAPE_FIELD_NUMBER: _ClassVar[int]
    EDGE_STYLE_FIELD_NUMBER: _ClassVar[int]
    port_type: PortType
    color: str
    display_name: str
    compatible_from: _containers.RepeatedScalarFieldContainer[PortType]
    handle_shape: HandleShape
    edge_style: EdgeStyle
    def __init__(self, port_type: _Optional[_Union[PortType, str]] = ..., color: _Optional[str] = ..., display_name: _Optional[str] = ..., compatible_from: _Optional[_Iterable[_Union[PortType, str]]] = ..., handle_shape: _Optional[_Union[HandleShape, str]] = ..., edge_style: _Optional[_Union[EdgeStyle, str]] = ...) -> None: ...

class GetRegistryDataRequest(_message.Message):
    __slots__ = ("registry",)
    REGISTRY_FIELD_NUMBER: _ClassVar[int]
    registry: str
    def __init__(self, registry: _Optional[str] = ...) -> None: ...

class RegistryEntry(_message.Message):
    __slots__ = ("id", "display_name", "icon", "category")
    ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    ICON_FIELD_NUMBER: _ClassVar[int]
    CATEGORY_FIELD_NUMBER: _ClassVar[int]
    id: str
    display_name: str
    icon: str
    category: str
    def __init__(self, id: _Optional[str] = ..., display_name: _Optional[str] = ..., icon: _Optional[str] = ..., category: _Optional[str] = ...) -> None: ...

class ValidateScriptRequest(_message.Message):
    __slots__ = ("instance_id", "nodes", "edges")
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    NODES_FIELD_NUMBER: _ClassVar[int]
    EDGES_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    nodes: _containers.RepeatedCompositeFieldContainer[ScriptNode]
    edges: _containers.RepeatedCompositeFieldContainer[ScriptEdge]
    def __init__(self, instance_id: _Optional[str] = ..., nodes: _Optional[_Iterable[_Union[ScriptNode, _Mapping]]] = ..., edges: _Optional[_Iterable[_Union[ScriptEdge, _Mapping]]] = ...) -> None: ...

class ValidateScriptResponse(_message.Message):
    __slots__ = ("diagnostics",)
    DIAGNOSTICS_FIELD_NUMBER: _ClassVar[int]
    diagnostics: _containers.RepeatedCompositeFieldContainer[ValidationDiagnostic]
    def __init__(self, diagnostics: _Optional[_Iterable[_Union[ValidationDiagnostic, _Mapping]]] = ...) -> None: ...

class DryRunScriptRequest(_message.Message):
    __slots__ = ("instance_id", "script_id", "trigger_node_id", "mock_inputs")
    class MockInputsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: _struct_pb2.Value
        def __init__(self, key: _Optional[str] = ..., value: _Optional[_Union[_struct_pb2.Value, _Mapping]] = ...) -> None: ...
    INSTANCE_ID_FIELD_NUMBER: _ClassVar[int]
    SCRIPT_ID_FIELD_NUMBER: _ClassVar[int]
    TRIGGER_NODE_ID_FIELD_NUMBER: _ClassVar[int]
    MOCK_INPUTS_FIELD_NUMBER: _ClassVar[int]
    instance_id: str
    script_id: str
    trigger_node_id: str
    mock_inputs: _containers.MessageMap[str, _struct_pb2.Value]
    def __init__(self, instance_id: _Optional[str] = ..., script_id: _Optional[str] = ..., trigger_node_id: _Optional[str] = ..., mock_inputs: _Optional[_Mapping[str, _struct_pb2.Value]] = ...) -> None: ...

class GetRegistryDataResponse(_message.Message):
    __slots__ = ("blocks", "entities", "items", "biomes")
    BLOCKS_FIELD_NUMBER: _ClassVar[int]
    ENTITIES_FIELD_NUMBER: _ClassVar[int]
    ITEMS_FIELD_NUMBER: _ClassVar[int]
    BIOMES_FIELD_NUMBER: _ClassVar[int]
    blocks: _containers.RepeatedCompositeFieldContainer[RegistryEntry]
    entities: _containers.RepeatedCompositeFieldContainer[RegistryEntry]
    items: _containers.RepeatedCompositeFieldContainer[RegistryEntry]
    biomes: _containers.RepeatedCompositeFieldContainer[RegistryEntry]
    def __init__(self, blocks: _Optional[_Iterable[_Union[RegistryEntry, _Mapping]]] = ..., entities: _Optional[_Iterable[_Union[RegistryEntry, _Mapping]]] = ..., items: _Optional[_Iterable[_Union[RegistryEntry, _Mapping]]] = ..., biomes: _Optional[_Iterable[_Union[RegistryEntry, _Mapping]]] = ...) -> None: ...
