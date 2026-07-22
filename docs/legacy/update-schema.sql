ALTER TABLE act_app_appdef
DROP
CONSTRAINT act_fk_app_def_dply;

ALTER TABLE act_app_deployment_resource
DROP
CONSTRAINT act_fk_app_rsrc_dpl;

ALTER TABLE act_ru_identitylink
DROP
CONSTRAINT act_fk_athrz_procedef;

ALTER TABLE act_ge_bytearray
DROP
CONSTRAINT act_fk_bytearr_depl;

ALTER TABLE act_cmmn_casedef
DROP
CONSTRAINT act_fk_case_def_dply;

ALTER TABLE act_cmmn_ru_case_inst
DROP
CONSTRAINT act_fk_case_inst_case_def;

ALTER TABLE act_cmmn_deployment_resource
DROP
CONSTRAINT act_fk_cmmn_rsrc_dpl;

ALTER TABLE act_ru_deadletter_job
DROP
CONSTRAINT act_fk_deadletter_job_custom_values;

ALTER TABLE act_ru_deadletter_job
DROP
CONSTRAINT act_fk_deadletter_job_exception;

ALTER TABLE act_ru_deadletter_job
DROP
CONSTRAINT act_fk_deadletter_job_execution;

ALTER TABLE act_ru_deadletter_job
DROP
CONSTRAINT act_fk_deadletter_job_proc_def;

ALTER TABLE act_ru_deadletter_job
DROP
CONSTRAINT act_fk_deadletter_job_process_instance;

ALTER TABLE act_ru_event_subscr
DROP
CONSTRAINT act_fk_event_exec;

ALTER TABLE act_ru_execution
DROP
CONSTRAINT act_fk_exe_parent;

ALTER TABLE act_ru_execution
DROP
CONSTRAINT act_fk_exe_procdef;

ALTER TABLE act_ru_execution
DROP
CONSTRAINT act_fk_exe_procinst;

ALTER TABLE act_ru_execution
DROP
CONSTRAINT act_fk_exe_super;

ALTER TABLE act_ru_external_job
DROP
CONSTRAINT act_fk_external_job_custom_values;

ALTER TABLE act_ru_external_job
DROP
CONSTRAINT act_fk_external_job_exception;

ALTER TABLE act_ru_identitylink
DROP
CONSTRAINT act_fk_idl_procinst;

ALTER TABLE act_procdef_info
DROP
CONSTRAINT act_fk_info_json_ba;

ALTER TABLE act_procdef_info
DROP
CONSTRAINT act_fk_info_procdef;

ALTER TABLE act_ru_job
DROP
CONSTRAINT act_fk_job_custom_values;

ALTER TABLE act_ru_job
DROP
CONSTRAINT act_fk_job_exception;

ALTER TABLE act_ru_job
DROP
CONSTRAINT act_fk_job_execution;

ALTER TABLE act_ru_job
DROP
CONSTRAINT act_fk_job_proc_def;

ALTER TABLE act_ru_job
DROP
CONSTRAINT act_fk_job_process_instance;

ALTER TABLE act_id_membership
DROP
CONSTRAINT act_fk_memb_group;

ALTER TABLE act_id_membership
DROP
CONSTRAINT act_fk_memb_user;

ALTER TABLE act_cmmn_ru_mil_inst
DROP
CONSTRAINT act_fk_mil_case_def;

ALTER TABLE act_cmmn_ru_mil_inst
DROP
CONSTRAINT act_fk_mil_case_inst;

ALTER TABLE act_re_model
DROP
CONSTRAINT act_fk_model_deployment;

ALTER TABLE act_re_model
DROP
CONSTRAINT act_fk_model_source;

ALTER TABLE act_re_model
DROP
CONSTRAINT act_fk_model_source_extra;

ALTER TABLE act_cmmn_ru_plan_item_inst
DROP
CONSTRAINT act_fk_plan_item_case_def;

ALTER TABLE act_cmmn_ru_plan_item_inst
DROP
CONSTRAINT act_fk_plan_item_case_inst;

ALTER TABLE act_id_priv_mapping
DROP
CONSTRAINT act_fk_priv_mapping;

ALTER TABLE act_cmmn_ru_sentry_part_inst
DROP
CONSTRAINT act_fk_sentry_case_def;

ALTER TABLE act_cmmn_ru_sentry_part_inst
DROP
CONSTRAINT act_fk_sentry_case_inst;

ALTER TABLE act_cmmn_ru_sentry_part_inst
DROP
CONSTRAINT act_fk_sentry_plan_item;

ALTER TABLE act_ru_suspended_job
DROP
CONSTRAINT act_fk_suspended_job_custom_values;

ALTER TABLE act_ru_suspended_job
DROP
CONSTRAINT act_fk_suspended_job_exception;

ALTER TABLE act_ru_suspended_job
DROP
CONSTRAINT act_fk_suspended_job_execution;

ALTER TABLE act_ru_suspended_job
DROP
CONSTRAINT act_fk_suspended_job_proc_def;

ALTER TABLE act_ru_suspended_job
DROP
CONSTRAINT act_fk_suspended_job_process_instance;

ALTER TABLE act_ru_task
DROP
CONSTRAINT act_fk_task_exe;

ALTER TABLE act_ru_task
DROP
CONSTRAINT act_fk_task_procdef;

ALTER TABLE act_ru_task
DROP
CONSTRAINT act_fk_task_procinst;

ALTER TABLE act_ru_timer_job
DROP
CONSTRAINT act_fk_timer_job_custom_values;

ALTER TABLE act_ru_timer_job
DROP
CONSTRAINT act_fk_timer_job_exception;

ALTER TABLE act_ru_timer_job
DROP
CONSTRAINT act_fk_timer_job_execution;

ALTER TABLE act_ru_timer_job
DROP
CONSTRAINT act_fk_timer_job_proc_def;

ALTER TABLE act_ru_timer_job
DROP
CONSTRAINT act_fk_timer_job_process_instance;

ALTER TABLE act_ru_identitylink
DROP
CONSTRAINT act_fk_tskass_task;

ALTER TABLE act_ru_variable
DROP
CONSTRAINT act_fk_var_bytearray;

ALTER TABLE act_ru_variable
DROP
CONSTRAINT act_fk_var_exe;

ALTER TABLE act_ru_variable
DROP
CONSTRAINT act_fk_var_procinst;

ALTER TABLE flw_ru_batch_part
DROP
CONSTRAINT flw_fk_batch_part_parent;

CREATE SEQUENCE IF NOT EXISTS beneficiary_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE beneficiary
(
    id             BIGINT NOT NULL,
    first_name     VARCHAR(255),
    last_name      VARCHAR(255),
    account_number VARCHAR(255),
    bank_name      VARCHAR(255),
    code           VARCHAR(255),
    CONSTRAINT pk_beneficiary PRIMARY KEY (id)
);

DROP TABLE act_app_appdef CASCADE;

DROP TABLE act_app_deployment CASCADE;

DROP TABLE act_app_deployment_resource CASCADE;

DROP TABLE act_cmmn_casedef CASCADE;

DROP TABLE act_cmmn_deployment CASCADE;

DROP TABLE act_cmmn_deployment_resource CASCADE;

DROP TABLE act_cmmn_hi_case_inst CASCADE;

DROP TABLE act_cmmn_hi_mil_inst CASCADE;

DROP TABLE act_cmmn_hi_plan_item_inst CASCADE;

DROP TABLE act_cmmn_ru_case_inst CASCADE;

DROP TABLE act_cmmn_ru_mil_inst CASCADE;

DROP TABLE act_cmmn_ru_plan_item_inst CASCADE;

DROP TABLE act_cmmn_ru_sentry_part_inst CASCADE;

DROP TABLE act_dmn_decision CASCADE;

DROP TABLE act_dmn_deployment CASCADE;

DROP TABLE act_dmn_deployment_resource CASCADE;

DROP TABLE act_dmn_hi_decision_execution CASCADE;

DROP TABLE act_evt_log CASCADE;

DROP TABLE act_ge_bytearray CASCADE;

DROP TABLE act_ge_property CASCADE;

DROP TABLE act_hi_actinst CASCADE;

DROP TABLE act_hi_attachment CASCADE;

DROP TABLE act_hi_comment CASCADE;

DROP TABLE act_hi_detail CASCADE;

DROP TABLE act_hi_entitylink CASCADE;

DROP TABLE act_hi_identitylink CASCADE;

DROP TABLE act_hi_procinst CASCADE;

DROP TABLE act_hi_taskinst CASCADE;

DROP TABLE act_hi_tsk_log CASCADE;

DROP TABLE act_hi_varinst CASCADE;

DROP TABLE act_id_bytearray CASCADE;

DROP TABLE act_id_group CASCADE;

DROP TABLE act_id_info CASCADE;

DROP TABLE act_id_membership CASCADE;

DROP TABLE act_id_priv CASCADE;

DROP TABLE act_id_priv_mapping CASCADE;

DROP TABLE act_id_property CASCADE;

DROP TABLE act_id_token CASCADE;

DROP TABLE act_id_user CASCADE;

DROP TABLE act_procdef_info CASCADE;

DROP TABLE act_re_deployment CASCADE;

DROP TABLE act_re_model CASCADE;

DROP TABLE act_re_procdef CASCADE;

DROP TABLE act_ru_actinst CASCADE;

DROP TABLE act_ru_deadletter_job CASCADE;

DROP TABLE act_ru_entitylink CASCADE;

DROP TABLE act_ru_event_subscr CASCADE;

DROP TABLE act_ru_execution CASCADE;

DROP TABLE act_ru_external_job CASCADE;

DROP TABLE act_ru_history_job CASCADE;

DROP TABLE act_ru_identitylink CASCADE;

DROP TABLE act_ru_job CASCADE;

DROP TABLE act_ru_suspended_job CASCADE;

DROP TABLE act_ru_task CASCADE;

DROP TABLE act_ru_timer_job CASCADE;

DROP TABLE act_ru_variable CASCADE;

DROP TABLE customers CASCADE;

DROP TABLE flw_channel_definition CASCADE;

DROP TABLE flw_event_definition CASCADE;

DROP TABLE flw_event_deployment CASCADE;

DROP TABLE flw_event_resource CASCADE;

DROP TABLE flw_ru_batch CASCADE;

DROP TABLE flw_ru_batch_part CASCADE;

DROP TABLE images CASCADE;

DROP TABLE services CASCADE;

DROP SEQUENCE journal_line_seq CASCADE;

DROP SEQUENCE primary_sequence CASCADE;