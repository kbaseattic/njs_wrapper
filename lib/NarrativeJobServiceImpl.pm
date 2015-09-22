package NarrativeJobServiceImpl;
use strict;
use Bio::KBase::Exceptions;
# Use Semantic Versioning (2.0.0-rc.1)
# http://semver.org 
our $VERSION = "0.1.0";

=head1 NAME

NarrativeJobService

=head1 DESCRIPTION



=cut

#BEGIN_HEADER
#END_HEADER

sub new
{
    my($class, @args) = @_;
    my $self = {
    };
    bless $self, $class;
    #BEGIN_CONSTRUCTOR
    #END_CONSTRUCTOR

    if ($self->can('_init_instance'))
    {
	$self->_init_instance();
    }
    return $self;
}

=head1 METHODS



=head2 run_app

  $return = $obj->run_app($app)

=over 4

=item Parameter and return types

=begin html

<pre>
$app is a NarrativeJobService.app
$return is a NarrativeJobService.app_state
app is a reference to a hash where the following keys are defined:
	name has a value which is a string
	steps has a value which is a reference to a list where each element is a NarrativeJobService.step
step is a reference to a hash where the following keys are defined:
	step_id has a value which is a string
	type has a value which is a string
	service has a value which is a NarrativeJobService.service_method
	script has a value which is a NarrativeJobService.script_method
	parameters has a value which is a reference to a list where each element is a NarrativeJobService.step_parameter
	input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	is_long_running has a value which is a NarrativeJobService.boolean
	job_id_output_field has a value which is a string
service_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	service_url has a value which is a string
	service_version has a value which is a string
script_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	has_files has a value which is a NarrativeJobService.boolean
boolean is an int
step_parameter is a reference to a hash where the following keys are defined:
	label has a value which is a string
	value has a value which is a string
	step_source has a value which is a string
	is_workspace_id has a value which is a NarrativeJobService.boolean
	ws_object has a value which is a NarrativeJobService.workspace_object
workspace_object is a reference to a hash where the following keys are defined:
	workspace_name has a value which is a string
	object_type has a value which is a string
	is_input has a value which is a NarrativeJobService.boolean
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a NarrativeJobService.job_id
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string
	is_deleted has a value which is a NarrativeJobService.boolean
job_id is a string

</pre>

=end html

=begin text

$app is a NarrativeJobService.app
$return is a NarrativeJobService.app_state
app is a reference to a hash where the following keys are defined:
	name has a value which is a string
	steps has a value which is a reference to a list where each element is a NarrativeJobService.step
step is a reference to a hash where the following keys are defined:
	step_id has a value which is a string
	type has a value which is a string
	service has a value which is a NarrativeJobService.service_method
	script has a value which is a NarrativeJobService.script_method
	parameters has a value which is a reference to a list where each element is a NarrativeJobService.step_parameter
	input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	is_long_running has a value which is a NarrativeJobService.boolean
	job_id_output_field has a value which is a string
service_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	service_url has a value which is a string
	service_version has a value which is a string
script_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	has_files has a value which is a NarrativeJobService.boolean
boolean is an int
step_parameter is a reference to a hash where the following keys are defined:
	label has a value which is a string
	value has a value which is a string
	step_source has a value which is a string
	is_workspace_id has a value which is a NarrativeJobService.boolean
	ws_object has a value which is a NarrativeJobService.workspace_object
workspace_object is a reference to a hash where the following keys are defined:
	workspace_name has a value which is a string
	object_type has a value which is a string
	is_input has a value which is a NarrativeJobService.boolean
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a NarrativeJobService.job_id
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string
	is_deleted has a value which is a NarrativeJobService.boolean
job_id is a string


=end text



=item Description



=back

=cut

sub run_app
{
    my $self = shift;
    my($app) = @_;

    my @_bad_arguments;
    (ref($app) eq 'HASH') or push(@_bad_arguments, "Invalid type for argument \"app\" (value was \"$app\")");
    if (@_bad_arguments) {
	my $msg = "Invalid arguments passed to run_app:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'run_app');
    }

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($return);
    #BEGIN run_app
    #END run_app
    my @_bad_returns;
    (ref($return) eq 'HASH') or push(@_bad_returns, "Invalid type for return variable \"return\" (value was \"$return\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to run_app:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'run_app');
    }
    return($return);
}




=head2 check_app_state

  $return = $obj->check_app_state($job_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$job_id is a NarrativeJobService.job_id
$return is a NarrativeJobService.app_state
job_id is a string
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a NarrativeJobService.job_id
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string
	is_deleted has a value which is a NarrativeJobService.boolean
boolean is an int

</pre>

=end html

=begin text

$job_id is a NarrativeJobService.job_id
$return is a NarrativeJobService.app_state
job_id is a string
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a NarrativeJobService.job_id
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string
	is_deleted has a value which is a NarrativeJobService.boolean
boolean is an int


=end text



=item Description



=back

=cut

sub check_app_state
{
    my $self = shift;
    my($job_id) = @_;

    my @_bad_arguments;
    (!ref($job_id)) or push(@_bad_arguments, "Invalid type for argument \"job_id\" (value was \"$job_id\")");
    if (@_bad_arguments) {
	my $msg = "Invalid arguments passed to check_app_state:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'check_app_state');
    }

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($return);
    #BEGIN check_app_state
    #END check_app_state
    my @_bad_returns;
    (ref($return) eq 'HASH') or push(@_bad_returns, "Invalid type for return variable \"return\" (value was \"$return\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to check_app_state:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'check_app_state');
    }
    return($return);
}




=head2 suspend_app

  $status = $obj->suspend_app($job_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$job_id is a NarrativeJobService.job_id
$status is a string
job_id is a string

</pre>

=end html

=begin text

$job_id is a NarrativeJobService.job_id
$status is a string
job_id is a string


=end text



=item Description

status - 'success' or 'failure' of action

=back

=cut

sub suspend_app
{
    my $self = shift;
    my($job_id) = @_;

    my @_bad_arguments;
    (!ref($job_id)) or push(@_bad_arguments, "Invalid type for argument \"job_id\" (value was \"$job_id\")");
    if (@_bad_arguments) {
	my $msg = "Invalid arguments passed to suspend_app:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'suspend_app');
    }

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($status);
    #BEGIN suspend_app
    #END suspend_app
    my @_bad_returns;
    (!ref($status)) or push(@_bad_returns, "Invalid type for return variable \"status\" (value was \"$status\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to suspend_app:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'suspend_app');
    }
    return($status);
}




=head2 resume_app

  $status = $obj->resume_app($job_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$job_id is a NarrativeJobService.job_id
$status is a string
job_id is a string

</pre>

=end html

=begin text

$job_id is a NarrativeJobService.job_id
$status is a string
job_id is a string


=end text



=item Description



=back

=cut

sub resume_app
{
    my $self = shift;
    my($job_id) = @_;

    my @_bad_arguments;
    (!ref($job_id)) or push(@_bad_arguments, "Invalid type for argument \"job_id\" (value was \"$job_id\")");
    if (@_bad_arguments) {
	my $msg = "Invalid arguments passed to resume_app:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'resume_app');
    }

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($status);
    #BEGIN resume_app
    #END resume_app
    my @_bad_returns;
    (!ref($status)) or push(@_bad_returns, "Invalid type for return variable \"status\" (value was \"$status\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to resume_app:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'resume_app');
    }
    return($status);
}




=head2 delete_app

  $status = $obj->delete_app($job_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$job_id is a NarrativeJobService.job_id
$status is a string
job_id is a string

</pre>

=end html

=begin text

$job_id is a NarrativeJobService.job_id
$status is a string
job_id is a string


=end text



=item Description



=back

=cut

sub delete_app
{
    my $self = shift;
    my($job_id) = @_;

    my @_bad_arguments;
    (!ref($job_id)) or push(@_bad_arguments, "Invalid type for argument \"job_id\" (value was \"$job_id\")");
    if (@_bad_arguments) {
	my $msg = "Invalid arguments passed to delete_app:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'delete_app');
    }

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($status);
    #BEGIN delete_app
    #END delete_app
    my @_bad_returns;
    (!ref($status)) or push(@_bad_returns, "Invalid type for return variable \"status\" (value was \"$status\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to delete_app:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'delete_app');
    }
    return($status);
}




=head2 list_config

  $return = $obj->list_config()

=over 4

=item Parameter and return types

=begin html

<pre>
$return is a reference to a hash where the key is a string and the value is a string

</pre>

=end html

=begin text

$return is a reference to a hash where the key is a string and the value is a string


=end text



=item Description



=back

=cut

sub list_config
{
    my $self = shift;

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($return);
    #BEGIN list_config
    #END list_config
    my @_bad_returns;
    (ref($return) eq 'HASH') or push(@_bad_returns, "Invalid type for return variable \"return\" (value was \"$return\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to list_config:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'list_config');
    }
    return($return);
}




=head2 ver

  $return = $obj->ver()

=over 4

=item Parameter and return types

=begin html

<pre>
$return is a string

</pre>

=end html

=begin text

$return is a string


=end text



=item Description

Returns the current running version of the NarrativeJobService.

=back

=cut

sub ver
{
    my $self = shift;

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($return);
    #BEGIN ver
    #END ver
    my @_bad_returns;
    (!ref($return)) or push(@_bad_returns, "Invalid type for return variable \"return\" (value was \"$return\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to ver:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'ver');
    }
    return($return);
}




=head2 status

  $return = $obj->status()

=over 4

=item Parameter and return types

=begin html

<pre>
$return is a NarrativeJobService.Status
Status is a reference to a hash where the following keys are defined:
	reboot_mode has a value which is a NarrativeJobService.boolean
	stopping_mode has a value which is a NarrativeJobService.boolean
	running_tasks_total has a value which is an int
	running_tasks_per_user has a value which is a reference to a hash where the key is a string and the value is an int
	tasks_in_queue has a value which is an int
	config has a value which is a reference to a hash where the key is a string and the value is a string
	git_commit has a value which is a string
boolean is an int

</pre>

=end html

=begin text

$return is a NarrativeJobService.Status
Status is a reference to a hash where the following keys are defined:
	reboot_mode has a value which is a NarrativeJobService.boolean
	stopping_mode has a value which is a NarrativeJobService.boolean
	running_tasks_total has a value which is an int
	running_tasks_per_user has a value which is a reference to a hash where the key is a string and the value is an int
	tasks_in_queue has a value which is an int
	config has a value which is a reference to a hash where the key is a string and the value is a string
	git_commit has a value which is a string
boolean is an int


=end text



=item Description

Simply check the status of this service to see queue details

=back

=cut

sub status
{
    my $self = shift;

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($return);
    #BEGIN status
    #END status
    my @_bad_returns;
    (ref($return) eq 'HASH') or push(@_bad_returns, "Invalid type for return variable \"return\" (value was \"$return\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to status:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'status');
    }
    return($return);
}




=head2 list_running_apps

  $return = $obj->list_running_apps()

=over 4

=item Parameter and return types

=begin html

<pre>
$return is a reference to a list where each element is a NarrativeJobService.app_state
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a NarrativeJobService.job_id
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string
	is_deleted has a value which is a NarrativeJobService.boolean
job_id is a string
boolean is an int

</pre>

=end html

=begin text

$return is a reference to a list where each element is a NarrativeJobService.app_state
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a NarrativeJobService.job_id
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string
	is_deleted has a value which is a NarrativeJobService.boolean
job_id is a string
boolean is an int


=end text



=item Description



=back

=cut

sub list_running_apps
{
    my $self = shift;

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($return);
    #BEGIN list_running_apps
    #END list_running_apps
    my @_bad_returns;
    (ref($return) eq 'ARRAY') or push(@_bad_returns, "Invalid type for return variable \"return\" (value was \"$return\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to list_running_apps:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'list_running_apps');
    }
    return($return);
}




=head2 run_job

  $job_id = $obj->run_job($params)

=over 4

=item Parameter and return types

=begin html

<pre>
$params is a NarrativeJobService.RunJobParams
$job_id is a NarrativeJobService.job_id
RunJobParams is a reference to a hash where the following keys are defined:
	method has a value which is a string
	params has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	service_ver has a value which is a string
	rpc_context has a value which is a NarrativeJobService.RpcContext
	remote_url has a value which is a string
RpcContext is a reference to a hash where the following keys are defined:
	call_stack has a value which is a reference to a list where each element is a NarrativeJobService.MethodCall
	run_id has a value which is a string
MethodCall is a reference to a hash where the following keys are defined:
	time has a value which is a NarrativeJobService.timestamp
	method has a value which is a string
	job_id has a value which is a NarrativeJobService.job_id
timestamp is a string
job_id is a string

</pre>

=end html

=begin text

$params is a NarrativeJobService.RunJobParams
$job_id is a NarrativeJobService.job_id
RunJobParams is a reference to a hash where the following keys are defined:
	method has a value which is a string
	params has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	service_ver has a value which is a string
	rpc_context has a value which is a NarrativeJobService.RpcContext
	remote_url has a value which is a string
RpcContext is a reference to a hash where the following keys are defined:
	call_stack has a value which is a reference to a list where each element is a NarrativeJobService.MethodCall
	run_id has a value which is a string
MethodCall is a reference to a hash where the following keys are defined:
	time has a value which is a NarrativeJobService.timestamp
	method has a value which is a string
	job_id has a value which is a NarrativeJobService.job_id
timestamp is a string
job_id is a string


=end text



=item Description

Start a new job (long running method of service registered in ServiceRegistery).
Such job runs Docker image for this service in script mode.

=back

=cut

sub run_job
{
    my $self = shift;
    my($params) = @_;

    my @_bad_arguments;
    (ref($params) eq 'HASH') or push(@_bad_arguments, "Invalid type for argument \"params\" (value was \"$params\")");
    if (@_bad_arguments) {
	my $msg = "Invalid arguments passed to run_job:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'run_job');
    }

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($job_id);
    #BEGIN run_job
    #END run_job
    my @_bad_returns;
    (!ref($job_id)) or push(@_bad_returns, "Invalid type for return variable \"job_id\" (value was \"$job_id\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to run_job:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'run_job');
    }
    return($job_id);
}




=head2 check_job

  $job_state = $obj->check_job($job_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$job_id is a NarrativeJobService.job_id
$job_state is a NarrativeJobService.JobState
job_id is a string
JobState is a reference to a hash where the following keys are defined:
	job_id has a value which is a string
	finished has a value which is a NarrativeJobService.boolean
	ujs_url has a value which is a string
	status has a value which is an UnspecifiedObject, which can hold any non-null object
	result has a value which is an UnspecifiedObject, which can hold any non-null object
	error has a value which is a NarrativeJobService.JsonRpcError
boolean is an int
JsonRpcError is a reference to a hash where the following keys are defined:
	name has a value which is a string
	code has a value which is an int
	message has a value which is a string
	error has a value which is a string

</pre>

=end html

=begin text

$job_id is a NarrativeJobService.job_id
$job_state is a NarrativeJobService.JobState
job_id is a string
JobState is a reference to a hash where the following keys are defined:
	job_id has a value which is a string
	finished has a value which is a NarrativeJobService.boolean
	ujs_url has a value which is a string
	status has a value which is an UnspecifiedObject, which can hold any non-null object
	result has a value which is an UnspecifiedObject, which can hold any non-null object
	error has a value which is a NarrativeJobService.JsonRpcError
boolean is an int
JsonRpcError is a reference to a hash where the following keys are defined:
	name has a value which is a string
	code has a value which is an int
	message has a value which is a string
	error has a value which is a string


=end text



=item Description

Check if a job is finished and get results/error

=back

=cut

sub check_job
{
    my $self = shift;
    my($job_id) = @_;

    my @_bad_arguments;
    (!ref($job_id)) or push(@_bad_arguments, "Invalid type for argument \"job_id\" (value was \"$job_id\")");
    if (@_bad_arguments) {
	my $msg = "Invalid arguments passed to check_job:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'check_job');
    }

    my $ctx = $NarrativeJobServiceServer::CallContext;
    my($job_state);
    #BEGIN check_job
    #END check_job
    my @_bad_returns;
    (ref($job_state) eq 'HASH') or push(@_bad_returns, "Invalid type for return variable \"job_state\" (value was \"$job_state\")");
    if (@_bad_returns) {
	my $msg = "Invalid returns passed to check_job:\n" . join("", map { "\t$_\n" } @_bad_returns);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'check_job');
    }
    return($job_state);
}




=head2 version 

  $return = $obj->version()

=over 4

=item Parameter and return types

=begin html

<pre>
$return is a string
</pre>

=end html

=begin text

$return is a string

=end text

=item Description

Return the module version. This is a Semantic Versioning number.

=back

=cut

sub version {
    return $VERSION;
}

=head1 TYPES



=head2 boolean

=over 4



=item Description

@range [0,1]


=item Definition

=begin html

<pre>
an int
</pre>

=end html

=begin text

an int

=end text

=back



=head2 timestamp

=over 4



=item Description

A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is either the
character Z (representing the UTC timezone) or the difference
in time to UTC in the format +/-HHMM, eg:
    2012-12-17T23:24:06-0500 (EST time)
    2013-04-03T08:56:32+0000 (UTC time)
    2013-04-03T08:56:32Z (UTC time)


=item Definition

=begin html

<pre>
a string
</pre>

=end html

=begin text

a string

=end text

=back



=head2 job_id

=over 4



=item Description

A job id.


=item Definition

=begin html

<pre>
a string
</pre>

=end html

=begin text

a string

=end text

=back



=head2 service_method

=over 4



=item Description

service_url could be empty in case of docker image of service loaded from registry,
service_version - optional parameter defining version of service docker image.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
service_name has a value which is a string
method_name has a value which is a string
service_url has a value which is a string
service_version has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
service_name has a value which is a string
method_name has a value which is a string
service_url has a value which is a string
service_version has a value which is a string


=end text

=back



=head2 script_method

=over 4



=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
service_name has a value which is a string
method_name has a value which is a string
has_files has a value which is a NarrativeJobService.boolean

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
service_name has a value which is a string
method_name has a value which is a string
has_files has a value which is a NarrativeJobService.boolean


=end text

=back



=head2 workspace_object

=over 4



=item Description

label - label of parameter, can be empty string for positional parameters
value - value of parameter
step_source - step_id that parameter derives from
is_workspace_id - parameter is a workspace id (value is object name)
# the below are only used if is_workspace_id is true
    is_input - parameter is an input (true) or output (false)
    workspace_name - name of workspace
    object_type - name of object type


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
workspace_name has a value which is a string
object_type has a value which is a string
is_input has a value which is a NarrativeJobService.boolean

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
workspace_name has a value which is a string
object_type has a value which is a string
is_input has a value which is a NarrativeJobService.boolean


=end text

=back



=head2 step_parameter

=over 4



=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
label has a value which is a string
value has a value which is a string
step_source has a value which is a string
is_workspace_id has a value which is a NarrativeJobService.boolean
ws_object has a value which is a NarrativeJobService.workspace_object

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
label has a value which is a string
value has a value which is a string
step_source has a value which is a string
is_workspace_id has a value which is a NarrativeJobService.boolean
ws_object has a value which is a NarrativeJobService.workspace_object


=end text

=back



=head2 step

=over 4



=item Description

type - 'service' or 'script'.
job_id_output_field - this field is used only in case this step is long running job and
    output of service method is structure with field having name coded in 
    'job_id_output_field' rather than just output string with job id.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
step_id has a value which is a string
type has a value which is a string
service has a value which is a NarrativeJobService.service_method
script has a value which is a NarrativeJobService.script_method
parameters has a value which is a reference to a list where each element is a NarrativeJobService.step_parameter
input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
is_long_running has a value which is a NarrativeJobService.boolean
job_id_output_field has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
step_id has a value which is a string
type has a value which is a string
service has a value which is a NarrativeJobService.service_method
script has a value which is a NarrativeJobService.script_method
parameters has a value which is a reference to a list where each element is a NarrativeJobService.step_parameter
input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
is_long_running has a value which is a NarrativeJobService.boolean
job_id_output_field has a value which is a string


=end text

=back



=head2 app

=over 4



=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
name has a value which is a string
steps has a value which is a reference to a list where each element is a NarrativeJobService.step

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
name has a value which is a string
steps has a value which is a reference to a list where each element is a NarrativeJobService.step


=end text

=back



=head2 app_state

=over 4



=item Description

job_id - id of job running app
job_state - 'queued', 'running', 'completed', or 'error'
running_step_id - id of step currently running
step_outputs - mapping step_id to stdout text produced by step, only for completed or errored steps
step_outputs - mapping step_id to stderr text produced by step, only for completed or errored steps
step_job_ids - mapping from step_id to job_id.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
job_id has a value which is a NarrativeJobService.job_id
job_state has a value which is a string
running_step_id has a value which is a string
step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
step_errors has a value which is a reference to a hash where the key is a string and the value is a string
is_deleted has a value which is a NarrativeJobService.boolean

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
job_id has a value which is a NarrativeJobService.job_id
job_state has a value which is a string
running_step_id has a value which is a string
step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
step_errors has a value which is a reference to a hash where the key is a string and the value is a string
is_deleted has a value which is a NarrativeJobService.boolean


=end text

=back



=head2 Status

=over 4



=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
reboot_mode has a value which is a NarrativeJobService.boolean
stopping_mode has a value which is a NarrativeJobService.boolean
running_tasks_total has a value which is an int
running_tasks_per_user has a value which is a reference to a hash where the key is a string and the value is an int
tasks_in_queue has a value which is an int
config has a value which is a reference to a hash where the key is a string and the value is a string
git_commit has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
reboot_mode has a value which is a NarrativeJobService.boolean
stopping_mode has a value which is a NarrativeJobService.boolean
running_tasks_total has a value which is an int
running_tasks_per_user has a value which is a reference to a hash where the key is a string and the value is an int
tasks_in_queue has a value which is an int
config has a value which is a reference to a hash where the key is a string and the value is a string
git_commit has a value which is a string


=end text

=back



=head2 MethodCall

=over 4



=item Description

time - the time the call was started;
method - service defined in standard JSON RPC way, typically it's
    module name from spec-file followed by '.' and name of funcdef
    from spec-file corresponding to running method (e.g.
    'KBaseTrees.construct_species_tree' from trees service);
job_id - job id if method is asynchronous (optional field).


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
time has a value which is a NarrativeJobService.timestamp
method has a value which is a string
job_id has a value which is a NarrativeJobService.job_id

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
time has a value which is a NarrativeJobService.timestamp
method has a value which is a string
job_id has a value which is a NarrativeJobService.job_id


=end text

=back



=head2 RpcContext

=over 4



=item Description

call_stack - upstream calls details including nested service calls and 
    parent jobs where calls are listed in order from outer to inner.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
call_stack has a value which is a reference to a list where each element is a NarrativeJobService.MethodCall
run_id has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
call_stack has a value which is a reference to a list where each element is a NarrativeJobService.MethodCall
run_id has a value which is a string


=end text

=back



=head2 RunJobParams

=over 4



=item Description

method - service defined in standard JSON RPC way, typically it's
    module name from spec-file followed by '.' and name of funcdef 
    from spec-file corresponding to running method (e.g.
    'KBaseTrees.construct_species_tree' from trees service);
params - the parameters of the method that performed this call;
service_ver - specific version of deployed service, last version is used 
    if this parameter is not defined (optional field);
rpc_context - context of current method call including nested call history
    (optional field, could be omitted in case there is no call history);
remote_url - optional field determining remote service call instead of
    local command line execution.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
method has a value which is a string
params has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
service_ver has a value which is a string
rpc_context has a value which is a NarrativeJobService.RpcContext
remote_url has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
method has a value which is a string
params has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
service_ver has a value which is a string
rpc_context has a value which is a NarrativeJobService.RpcContext
remote_url has a value which is a string


=end text

=back



=head2 JsonRpcError

=over 4



=item Description

Error block of JSON RPC response


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
name has a value which is a string
code has a value which is an int
message has a value which is a string
error has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
name has a value which is a string
code has a value which is an int
message has a value which is a string
error has a value which is a string


=end text

=back



=head2 FinishJobParams

=over 4



=item Description

Either 'result' or 'error' field should be defined;
result - keeps exact copy of what original server method puts
    in result block of JSON RPC response;
error - keeps exact copy of what original server method puts
    in error block of JSON RPC response.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
result has a value which is an UnspecifiedObject, which can hold any non-null object
error has a value which is a NarrativeJobService.JsonRpcError

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
result has a value which is an UnspecifiedObject, which can hold any non-null object
error has a value which is a NarrativeJobService.JsonRpcError


=end text

=back



=head2 JobState

=over 4



=item Description

job_id - id of job running method
finished - indicates whether job is done (including error cases) or not,
    if the value is true then either of 'returned_data' or 'detailed_error'
    should be defined;
ujs_url - url of UserAndJobState service used by job service
status - tuple returned by UserAndJobState.get_job_status method
result - keeps exact copy of what original server method puts
    in result block of JSON RPC response;
error - keeps exact copy of what original server method puts
    in error block of JSON RPC response.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
job_id has a value which is a string
finished has a value which is a NarrativeJobService.boolean
ujs_url has a value which is a string
status has a value which is an UnspecifiedObject, which can hold any non-null object
result has a value which is an UnspecifiedObject, which can hold any non-null object
error has a value which is a NarrativeJobService.JsonRpcError

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
job_id has a value which is a string
finished has a value which is a NarrativeJobService.boolean
ujs_url has a value which is a string
status has a value which is an UnspecifiedObject, which can hold any non-null object
result has a value which is an UnspecifiedObject, which can hold any non-null object
error has a value which is a NarrativeJobService.JsonRpcError


=end text

=back



=cut

1;
