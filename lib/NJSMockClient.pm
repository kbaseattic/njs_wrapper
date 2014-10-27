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
        else
        {
	    #
	    # All methods in this module require authentication. In this case, if we
	    # don't have a token, we can't continue.
	    #
	    die "Authentication failed: " . $token->error_message;
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
	app_run_id has a value which is a string
	steps has a value which is a reference to a list where each element is an NJSMock.step
step is a reference to a hash where the following keys are defined:
	step_id has a value which is a string
	type has a value which is a string
	generic has a value which is an NJSMock.generic_service_method
	python has a value which is an NJSMock.python_backend_method
	script has a value which is an NJSMock.commandline_script_method
	input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	is_long_running has a value which is an NJSMock.boolean
	job_id_output_field has a value which is a string
generic_service_method is a reference to a hash where the following keys are defined:
	service_url has a value which is a string
	method_name has a value which is a string
python_backend_method is a reference to a hash where the following keys are defined:
	python_class has a value which is a string
	method_name has a value which is a string
commandline_script_method is a reference to a hash where the following keys are defined:
	script_name has a value which is a string
boolean is an int
app_state is a reference to a hash where the following keys are defined:
	app_job_id has a value which is a string
	running_step_id has a value which is a string
	step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is an UnspecifiedObject, which can hold any non-null object

</pre>

=end html

=begin text

$app is an NJSMock.app
$return is an NJSMock.app_state
app is a reference to a hash where the following keys are defined:
	app_run_id has a value which is a string
	steps has a value which is a reference to a list where each element is an NJSMock.step
step is a reference to a hash where the following keys are defined:
	step_id has a value which is a string
	type has a value which is a string
	generic has a value which is an NJSMock.generic_service_method
	python has a value which is an NJSMock.python_backend_method
	script has a value which is an NJSMock.commandline_script_method
	input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
	is_long_running has a value which is an NJSMock.boolean
	job_id_output_field has a value which is a string
generic_service_method is a reference to a hash where the following keys are defined:
	service_url has a value which is a string
	method_name has a value which is a string
python_backend_method is a reference to a hash where the following keys are defined:
	python_class has a value which is a string
	method_name has a value which is a string
commandline_script_method is a reference to a hash where the following keys are defined:
	script_name has a value which is a string
boolean is an int
app_state is a reference to a hash where the following keys are defined:
	app_job_id has a value which is a string
	running_step_id has a value which is a string
	step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is an UnspecifiedObject, which can hold any non-null object


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

  $return = $obj->check_app_state($app_run_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$app_run_id is a string
$return is an NJSMock.app_state
app_state is a reference to a hash where the following keys are defined:
	app_job_id has a value which is a string
	running_step_id has a value which is a string
	step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is an UnspecifiedObject, which can hold any non-null object

</pre>

=end html

=begin text

$app_run_id is a string
$return is an NJSMock.app_state
app_state is a reference to a hash where the following keys are defined:
	app_job_id has a value which is a string
	running_step_id has a value which is a string
	step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string
	step_outputs has a value which is a reference to a hash where the key is a string and the value is an UnspecifiedObject, which can hold any non-null object


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
	my($app_run_id) = @args;

	my @_bad_arguments;
        (!ref($app_run_id)) or push(@_bad_arguments, "Invalid type for argument 1 \"app_run_id\" (value was \"$app_run_id\")");
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
                method_name => 'check_app_state',
            );
        } else {
            return wantarray ? @{$result->result} : $result->result->[0];
        }
    } else {
        Bio::KBase::Exceptions::HTTP->throw(
            error => "Error invoking method check_app_state",
            status_line => $self->{client}->status_line,
            method_name => 'check_app_state',
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



=head2 generic_service_method

=over 4



=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
service_url has a value which is a string
method_name has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
service_url has a value which is a string
method_name has a value which is a string


=end text

=back



=head2 python_backend_method

=over 4



=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
python_class has a value which is a string
method_name has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
python_class has a value which is a string
method_name has a value which is a string


=end text

=back



=head2 commandline_script_method

=over 4



=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
script_name has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
script_name has a value which is a string


=end text

=back



=head2 step

=over 4



=item Description

type - 'generic', 'python' or 'script'.
job_id_output_field - this field is used only in case this step is long running job and
    output of service method is structure with field having name coded in 
    'job_id_output_field' rather than just output string with job id.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
step_id has a value which is a string
type has a value which is a string
generic has a value which is an NJSMock.generic_service_method
python has a value which is an NJSMock.python_backend_method
script has a value which is an NJSMock.commandline_script_method
input_values has a value which is a reference to a list where each element is an UnspecifiedObject, which can hold any non-null object
is_long_running has a value which is an NJSMock.boolean
job_id_output_field has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
step_id has a value which is a string
type has a value which is a string
generic has a value which is an NJSMock.generic_service_method
python has a value which is an NJSMock.python_backend_method
script has a value which is an NJSMock.commandline_script_method
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
app_run_id has a value which is a string
steps has a value which is a reference to a list where each element is an NJSMock.step

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
app_run_id has a value which is a string
steps has a value which is a reference to a list where each element is an NJSMock.step


=end text

=back



=head2 app_state

=over 4



=item Description

step_job_ids - mapping from step_id to job_id.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
app_job_id has a value which is a string
running_step_id has a value which is a string
step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string
step_outputs has a value which is a reference to a hash where the key is a string and the value is an UnspecifiedObject, which can hold any non-null object

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
app_job_id has a value which is a string
running_step_id has a value which is a string
step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string
step_outputs has a value which is a reference to a hash where the key is a string and the value is an UnspecifiedObject, which can hold any non-null object


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
