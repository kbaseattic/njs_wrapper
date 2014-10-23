package NJSMockImpl;
use strict;
use Bio::KBase::Exceptions;
# Use Semantic Versioning (2.0.0-rc.1)
# http://semver.org 
our $VERSION = "0.1.0";

=head1 NAME

NJSMock

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
$app is an NJSMock.app
$return is an NJSMock.app_jobs
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
generic_service_method is a reference to a hash where the following keys are defined:
	service_url has a value which is a string
	method_name has a value which is a string
python_backend_method is a reference to a hash where the following keys are defined:
	python_class has a value which is a string
	method_name has a value which is a string
commandline_script_method is a reference to a hash where the following keys are defined:
	script_name has a value which is a string
app_jobs is a reference to a hash where the following keys are defined:
	app_job_id has a value which is a string
	step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string

</pre>

=end html

=begin text

$app is an NJSMock.app
$return is an NJSMock.app_jobs
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
generic_service_method is a reference to a hash where the following keys are defined:
	service_url has a value which is a string
	method_name has a value which is a string
python_backend_method is a reference to a hash where the following keys are defined:
	python_class has a value which is a string
	method_name has a value which is a string
commandline_script_method is a reference to a hash where the following keys are defined:
	script_name has a value which is a string
app_jobs is a reference to a hash where the following keys are defined:
	app_job_id has a value which is a string
	step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string


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

    my $ctx = $NJSMockServer::CallContext;
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

  $return = $obj->check_app_state($app_run_id)

=over 4

=item Parameter and return types

=begin html

<pre>
$app_run_id is a string
$return is an NJSMock.app_jobs
app_jobs is a reference to a hash where the following keys are defined:
	app_job_id has a value which is a string
	step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string

</pre>

=end html

=begin text

$app_run_id is a string
$return is an NJSMock.app_jobs
app_jobs is a reference to a hash where the following keys are defined:
	app_job_id has a value which is a string
	step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string


=end text



=item Description



=back

=cut

sub check_app_state
{
    my $self = shift;
    my($app_run_id) = @_;

    my @_bad_arguments;
    (!ref($app_run_id)) or push(@_bad_arguments, "Invalid type for argument \"app_run_id\" (value was \"$app_run_id\")");
    if (@_bad_arguments) {
	my $msg = "Invalid arguments passed to check_app_state:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
							       method_name => 'check_app_state');
    }

    my $ctx = $NJSMockServer::CallContext;
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



=head2 app_jobs

=over 4



=item Description

step_jobs - mapping from step_id to job_id.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
app_job_id has a value which is a string
step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
app_job_id has a value which is a string
step_job_ids has a value which is a reference to a hash where the key is a string and the value is a string


=end text

=back



=cut

1;
