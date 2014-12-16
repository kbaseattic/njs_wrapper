package NJSMockClient;

use JSON::RPC::Client;
use strict;
use Data::Dumper;
use URI;
use Bio::KBase::Exceptions;
use Bio::KBase::AuthToken;

# Client version should match Impl version
# This is a Semantic Version number,
# http://semver.org
our $VERSION = "0.1.0";

=head1 NAME

NJSMockClient

=head1 DESCRIPTION





=cut

sub new
{
    my($class, $url, @args) = @_;
    

    my $self = {
	client => NJSMockClient::RpcClient->new,
	url => $url,
    };

    #
    # This module requires authentication.
    #
    # We create an auth token, passing through the arguments that we were (hopefully) given.

    {
	my $token = Bio::KBase::AuthToken->new(@args);
	
	if (!$token->error_message)
	{
	    $self->{token} = $token->token;
	    $self->{client}->{token} = $token->token;
	}
    }

    my $ua = $self->{client}->ua;	 
    my $timeout = $ENV{CDMI_TIMEOUT} || (30 * 60);	 
    $ua->timeout($timeout);
    bless $self, $class;
    #    $self->_validate_version();
    return $self;
}




=head2 run_app

  $return = $obj->run_app($app)

=over 4

=item Parameter and return types

=begin html

<pre>
$app is an NJSMock.app
$return is an NJSMock.app_state
app is a reference to a hash where the following keys are defined:
	name has a value which is a string
	steps has a value which is a reference to a list where each element is an NJSMock.step
step is a reference to a hash where the following keys are defined:
	step_id has a value which is a string
	type has a value which is a string
	service has a value which is an NJSMock.service_method
	script has a value which is an NJSMock.script_method
	parameters has a value which is a reference to a list where each element is an NJSMock.step_parameter
	input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	is_long_running has a value which is an NJSMock.boolean
	job_id_output_field has a value which is a string
service_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	service_url has a value which is a string
script_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	has_files has a value which is an NJSMock.boolean
boolean is an int
step_parameter is a reference to a hash where the following keys are defined:
	label has a value which is a string
	value has a value which is a string
	step_source has a value which is a string
	is_workspace_id has a value which is an NJSMock.boolean
	ws_object has a value which is an NJSMock.workspace_object
workspace_object is a reference to a hash where the following keys are defined:
	workspace_name has a value which is a string
	object_type has a value which is a string
	is_input has a value which is an NJSMock.boolean
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a string
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string

</pre>

=end html

=begin text

$app is an NJSMock.app
$return is an NJSMock.app_state
app is a reference to a hash where the following keys are defined:
	name has a value which is a string
	steps has a value which is a reference to a list where each element is an NJSMock.step
step is a reference to a hash where the following keys are defined:
	step_id has a value which is a string
	type has a value which is a string
	service has a value which is an NJSMock.service_method
	script has a value which is an NJSMock.script_method
	parameters has a value which is a reference to a list where each element is an NJSMock.step_parameter
	input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	is_long_running has a value which is an NJSMock.boolean
	job_id_output_field has a value which is a string
service_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	service_url has a value which is a string
script_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	has_files has a value which is an NJSMock.boolean
boolean is an int
step_parameter is a reference to a hash where the following keys are defined:
	label has a value which is a string
	value has a value which is a string
	step_source has a value which is a string
	is_workspace_id has a value which is an NJSMock.boolean
	ws_object has a value which is an NJSMock.workspace_object
workspace_object is a reference to a hash where the following keys are defined:
	workspace_name has a value which is a string
	object_type has a value which is a string
	is_input has a value which is an NJSMock.boolean
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a string
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string


=end text

=item Description



=back

=cut

sub run_app
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function run_app (received $n, expecting 1)");
    }
    {
	my($app) = @args;

	my @_bad_arguments;
        (ref($app) eq 'HASH') or push(@_bad_arguments, "Invalid type for argument 1 \"app\" (value was \"$app\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to run_app:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'run_app');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "NJSMock.run_app",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{error}->{code},
					       method_name => 'run_app',
					       data => $result->content->{error}->{error} # JSON::RPC::ReturnObject only supports JSONRPC 1.1 or 1.O
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method run_app",
					    status_line => $self->{client}->status_line,
					    method_name => 'run_app',
				       );
    }
}



=head2 check_app_state

  $return = $obj->check_app_state($job_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$job_id is a string
$return is an NJSMock.app_state
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a string
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string

</pre>

=end html

=begin text

$job_id is a string
$return is an NJSMock.app_state
app_state is a reference to a hash where the following keys are defined:
	job_id has a value which is a string
	job_state has a value which is a string
	running_step_id has a value which is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
	step_errors has a value which is a reference to a hash where the key is a string and the value is a string


=end text

=item Description



=back

=cut

sub check_app_state
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function check_app_state (received $n, expecting 1)");
    }
    {
	my($job_id) = @args;

	my @_bad_arguments;
        (!ref($job_id)) or push(@_bad_arguments, "Invalid type for argument 1 \"job_id\" (value was \"$job_id\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to check_app_state:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'check_app_state');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "NJSMock.check_app_state",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{error}->{code},
					       method_name => 'check_app_state',
					       data => $result->content->{error}->{error} # JSON::RPC::ReturnObject only supports JSONRPC 1.1 or 1.O
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method check_app_state",
					    status_line => $self->{client}->status_line,
					    method_name => 'check_app_state',
				       );
    }
}



=head2 run_step

  $ujs_job_id = $obj->run_step($step)

=over 4

=item Parameter and return types

=begin html

<pre>
$step is an NJSMock.step
$ujs_job_id is a string
step is a reference to a hash where the following keys are defined:
	step_id has a value which is a string
	type has a value which is a string
	service has a value which is an NJSMock.service_method
	script has a value which is an NJSMock.script_method
	parameters has a value which is a reference to a list where each element is an NJSMock.step_parameter
	input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	is_long_running has a value which is an NJSMock.boolean
	job_id_output_field has a value which is a string
service_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	service_url has a value which is a string
script_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	has_files has a value which is an NJSMock.boolean
boolean is an int
step_parameter is a reference to a hash where the following keys are defined:
	label has a value which is a string
	value has a value which is a string
	step_source has a value which is a string
	is_workspace_id has a value which is an NJSMock.boolean
	ws_object has a value which is an NJSMock.workspace_object
workspace_object is a reference to a hash where the following keys are defined:
	workspace_name has a value which is a string
	object_type has a value which is a string
	is_input has a value which is an NJSMock.boolean

</pre>

=end html

=begin text

$step is an NJSMock.step
$ujs_job_id is a string
step is a reference to a hash where the following keys are defined:
	step_id has a value which is a string
	type has a value which is a string
	service has a value which is an NJSMock.service_method
	script has a value which is an NJSMock.script_method
	parameters has a value which is a reference to a list where each element is an NJSMock.step_parameter
	input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	is_long_running has a value which is an NJSMock.boolean
	job_id_output_field has a value which is a string
service_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	service_url has a value which is a string
script_method is a reference to a hash where the following keys are defined:
	service_name has a value which is a string
	method_name has a value which is a string
	has_files has a value which is an NJSMock.boolean
boolean is an int
step_parameter is a reference to a hash where the following keys are defined:
	label has a value which is a string
	value has a value which is a string
	step_source has a value which is a string
	is_workspace_id has a value which is an NJSMock.boolean
	ws_object has a value which is an NJSMock.workspace_object
workspace_object is a reference to a hash where the following keys are defined:
	workspace_name has a value which is a string
	object_type has a value which is a string
	is_input has a value which is an NJSMock.boolean


=end text

=item Description



=back

=cut

sub run_step
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function run_step (received $n, expecting 1)");
    }
    {
	my($step) = @args;

	my @_bad_arguments;
        (ref($step) eq 'HASH') or push(@_bad_arguments, "Invalid type for argument 1 \"step\" (value was \"$step\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to run_step:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'run_step');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "NJSMock.run_step",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{error}->{code},
					       method_name => 'run_step',
					       data => $result->content->{error}->{error} # JSON::RPC::ReturnObject only supports JSONRPC 1.1 or 1.O
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method run_step",
					    status_line => $self->{client}->status_line,
					    method_name => 'run_step',
				       );
    }
}



=head2 suspend_app

  $status = $obj->suspend_app($job_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$job_id is a string
$status is a string

</pre>

=end html

=begin text

$job_id is a string
$status is a string


=end text

=item Description

status - 'success' or 'failure' of action

=back

=cut

sub suspend_app
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function suspend_app (received $n, expecting 1)");
    }
    {
	my($job_id) = @args;

	my @_bad_arguments;
        (!ref($job_id)) or push(@_bad_arguments, "Invalid type for argument 1 \"job_id\" (value was \"$job_id\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to suspend_app:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'suspend_app');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "NJSMock.suspend_app",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{error}->{code},
					       method_name => 'suspend_app',
					       data => $result->content->{error}->{error} # JSON::RPC::ReturnObject only supports JSONRPC 1.1 or 1.O
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method suspend_app",
					    status_line => $self->{client}->status_line,
					    method_name => 'suspend_app',
				       );
    }
}



=head2 resume_app

  $status = $obj->resume_app($job_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$job_id is a string
$status is a string

</pre>

=end html

=begin text

$job_id is a string
$status is a string


=end text

=item Description



=back

=cut

sub resume_app
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function resume_app (received $n, expecting 1)");
    }
    {
	my($job_id) = @args;

	my @_bad_arguments;
        (!ref($job_id)) or push(@_bad_arguments, "Invalid type for argument 1 \"job_id\" (value was \"$job_id\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to resume_app:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'resume_app');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "NJSMock.resume_app",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{error}->{code},
					       method_name => 'resume_app',
					       data => $result->content->{error}->{error} # JSON::RPC::ReturnObject only supports JSONRPC 1.1 or 1.O
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method resume_app",
					    status_line => $self->{client}->status_line,
					    method_name => 'resume_app',
				       );
    }
}



=head2 delete_app

  $status = $obj->delete_app($job_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$job_id is a string
$status is a string

</pre>

=end html

=begin text

$job_id is a string
$status is a string


=end text

=item Description



=back

=cut

sub delete_app
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function delete_app (received $n, expecting 1)");
    }
    {
	my($job_id) = @args;

	my @_bad_arguments;
        (!ref($job_id)) or push(@_bad_arguments, "Invalid type for argument 1 \"job_id\" (value was \"$job_id\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to delete_app:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'delete_app');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "NJSMock.delete_app",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{error}->{code},
					       method_name => 'delete_app',
					       data => $result->content->{error}->{error} # JSON::RPC::ReturnObject only supports JSONRPC 1.1 or 1.O
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method delete_app",
					    status_line => $self->{client}->status_line,
					    method_name => 'delete_app',
				       );
    }
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
    my($self, @args) = @_;

# Authentication: optional

    if ((my $n = @args) != 0)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function list_config (received $n, expecting 0)");
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "NJSMock.list_config",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{error}->{code},
					       method_name => 'list_config',
					       data => $result->content->{error}->{error} # JSON::RPC::ReturnObject only supports JSONRPC 1.1 or 1.O
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method list_config",
					    status_line => $self->{client}->status_line,
					    method_name => 'list_config',
				       );
    }
}



sub version {
    my ($self) = @_;
    my $result = $self->{client}->call($self->{url}, {
        method => "NJSMock.version",
        params => [],
    });
    if ($result) {
        if ($result->is_error) {
            Bio::KBase::Exceptions::JSONRPC->throw(
                error => $result->error_message,
                code => $result->content->{code},
                method_name => 'list_config',
            );
        } else {
            return wantarray ? @{$result->result} : $result->result->[0];
        }
    } else {
        Bio::KBase::Exceptions::HTTP->throw(
            error => "Error invoking method list_config",
            status_line => $self->{client}->status_line,
            method_name => 'list_config',
        );
    }
}

sub _validate_version {
    my ($self) = @_;
    my $svr_version = $self->version();
    my $client_version = $VERSION;
    my ($cMajor, $cMinor) = split(/\./, $client_version);
    my ($sMajor, $sMinor) = split(/\./, $svr_version);
    if ($sMajor != $cMajor) {
        Bio::KBase::Exceptions::ClientServerIncompatible->throw(
            error => "Major version numbers differ.",
            server_version => $svr_version,
            client_version => $client_version
        );
    }
    if ($sMinor < $cMinor) {
        Bio::KBase::Exceptions::ClientServerIncompatible->throw(
            error => "Client minor version greater than Server minor version.",
            server_version => $svr_version,
            client_version => $client_version
        );
    }
    if ($sMinor > $cMinor) {
        warn "New client version available for NJSMockClient\n";
    }
    if ($sMajor == 0) {
        warn "NJSMockClient version is $svr_version. API subject to change.\n";
    }
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



=head2 service_method

=over 4



=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
service_name has a value which is a string
method_name has a value which is a string
service_url has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
service_name has a value which is a string
method_name has a value which is a string
service_url has a value which is a string


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
has_files has a value which is an NJSMock.boolean

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
service_name has a value which is a string
method_name has a value which is a string
has_files has a value which is an NJSMock.boolean


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
is_input has a value which is an NJSMock.boolean

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
workspace_name has a value which is a string
object_type has a value which is a string
is_input has a value which is an NJSMock.boolean


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
is_workspace_id has a value which is an NJSMock.boolean
ws_object has a value which is an NJSMock.workspace_object

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
label has a value which is a string
value has a value which is a string
step_source has a value which is a string
is_workspace_id has a value which is an NJSMock.boolean
ws_object has a value which is an NJSMock.workspace_object


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
service has a value which is an NJSMock.service_method
script has a value which is an NJSMock.script_method
parameters has a value which is a reference to a list where each element is an NJSMock.step_parameter
input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
is_long_running has a value which is an NJSMock.boolean
job_id_output_field has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
step_id has a value which is a string
type has a value which is a string
service has a value which is an NJSMock.service_method
script has a value which is an NJSMock.script_method
parameters has a value which is a reference to a list where each element is an NJSMock.step_parameter
input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
is_long_running has a value which is an NJSMock.boolean
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
steps has a value which is a reference to a list where each element is an NJSMock.step

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
name has a value which is a string
steps has a value which is a reference to a list where each element is an NJSMock.step


=end text

=back



=head2 app_state

=over 4



=item Description

mapping<string, string> step_job_ids;


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
job_id has a value which is a string
job_state has a value which is a string
running_step_id has a value which is a string
step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
step_errors has a value which is a reference to a hash where the key is a string and the value is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
job_id has a value which is a string
job_state has a value which is a string
running_step_id has a value which is a string
step_outputs has a value which is a reference to a hash where the key is a string and the value is a string
step_errors has a value which is a reference to a hash where the key is a string and the value is a string


=end text

=back



=cut

package NJSMockClient::RpcClient;
use base 'JSON::RPC::Client';

#
# Override JSON::RPC::Client::call because it doesn't handle error returns properly.
#

sub call {
    my ($self, $uri, $obj) = @_;
    my $result;

    if ($uri =~ /\?/) {
       $result = $self->_get($uri);
    }
    else {
        Carp::croak "not hashref." unless (ref $obj eq 'HASH');
        $result = $self->_post($uri, $obj);
    }

    my $service = $obj->{method} =~ /^system\./ if ( $obj );

    $self->status_line($result->status_line);

    if ($result->is_success) {

        return unless($result->content); # notification?

        if ($service) {
            return JSON::RPC::ServiceObject->new($result, $self->json);
        }

        return JSON::RPC::ReturnObject->new($result, $self->json);
    }
    elsif ($result->content_type eq 'application/json')
    {
        return JSON::RPC::ReturnObject->new($result, $self->json);
    }
    else {
        return;
    }
}


sub _post {
    my ($self, $uri, $obj) = @_;
    my $json = $self->json;

    $obj->{version} ||= $self->{version} || '1.1';

    if ($obj->{version} eq '1.0') {
        delete $obj->{version};
        if (exists $obj->{id}) {
            $self->id($obj->{id}) if ($obj->{id}); # if undef, it is notification.
        }
        else {
            $obj->{id} = $self->id || ($self->id('JSON::RPC::Client'));
        }
    }
    else {
        # $obj->{id} = $self->id if (defined $self->id);
	# Assign a random number to the id if one hasn't been set
	$obj->{id} = (defined $self->id) ? $self->id : substr(rand(),2);
    }

    my $content = $json->encode($obj);

    $self->ua->post(
        $uri,
        Content_Type   => $self->{content_type},
        Content        => $content,
        Accept         => 'application/json',
	($self->{token} ? (Authorization => $self->{token}) : ()),
    );
}



1;
