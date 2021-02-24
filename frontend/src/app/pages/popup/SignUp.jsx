import PageComponent from "../../component/PageComponent";
import { Alert, Button, Card, CardBody, CardHeader, Col, CustomInput, Form, FormGroup, Row, FormText, Spinner, Input, Label } from 'reactstrap';


export default class SignUp extends PageComponent {
    constructor(props) {
        super(props)
        this.onFormSubmit = this.onFormSubmit.bind(this)
        this.onFormChange = this.onFormChange.bind(this)
        this.user = {
            username: '',
            email: '',
            firstname: '',
            lastname: '',
            password: '',
        }
    }

    initState() {

    }

    onFormChange(e) {
        e.preventDefault();
        console.log(this.user)
    }

    onFormSubmit(e) {
        e.preventDefault();

    }
    render() {
        return (
            <div>
                <Row>
                    <Col
                        sm={{ size: 8, offset: 4 }}
                        md={{ size: 7, offset: 5 }}
                        lg={{ size: 4, offset: 6 }}>
                        <Card>
                            <CardHeader>
                                <h1>Register</h1>
                            </CardHeader>
                            <CardBody>
                                <Form
                                    data-trackid="login-form"
                                    onSubmit={this.onFormSubmit}
                                >
                                    <br /><br /><br /><br /><br />
                                    <FormGroup>
                                        <Input
                                            required
                                            type="text"
                                            name="firstname"
                                            placeholder="First Name"
                                            value={this.user.firstname}
                                        />
                                    </FormGroup>
                                    <FormGroup>
                                        <Input
                                            required
                                            type="text"
                                            name="lastname"
                                            placeholder="Last name"
                                            value={this.user.lastname}
                                        />
                                    </FormGroup>
                                    <FormGroup>
                                        <Input
                                            required
                                            type="text"
                                            name="email"
                                            placeholder="Email"
                                            value={this.user.email}
                                        />
                                    </FormGroup>
                                    <FormGroup>
                                        <Input
                                            required
                                            type="password"
                                            name="password"
                                            placeholder="Password"
                                            value={this.user.password}
                                        />
                                    </FormGroup>
                                    <FormGroup>
                                        <Input
                                            required
                                            type="password"
                                            name="password"
                                            placeholder="Re-type password"
                                        />
                                    </FormGroup>
                                    <FormGroup>
                                        <Col xs="auto" className="mt-2">
                                            <Button type="submit" color="primary" className="px-4">
                                                Login
                                            </Button>
                                        </Col>
                                    </FormGroup>
                                    <p>
                                        Already have an account? <a href="/login">Login</a>
                                    </p>
                                    <br /><br /><br /><br /><br />
                                </Form>
                            </CardBody>
                        </Card>

                    </Col>

                </Row>
            </div>
        );
    }
}