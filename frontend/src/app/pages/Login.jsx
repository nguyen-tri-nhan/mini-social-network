import _ from 'lodash';
import React from 'react';
import PageComponent from "../component/PageComponent";
import { Alert, Button, Card, CardBody, CardHeader, Col, CustomInput, Form, FormGroup, Row, FormText, Spinner, Input, Label } from 'reactstrap';
import { Link } from 'react-router-dom';
import Popup from "reactjs-popup";

class Login extends PageComponent {

    user = null;
    constructor(props) {
        super(props);
        this.user = {
            username: '',
            password: '',
        }
        this.login = this.login.bind(this);
    }

    login(e) {

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
                                <h1>Login</h1>
                            </CardHeader>
                            <CardBody>
                                <Form data-trackid="login-form">
                                    <br /><br /><br /><br /><br /><br /><br /><br />
                                    <FormGroup>
                                        <Input
                                            required
                                            type="text"
                                            name="username"
                                            placeholder="Username or email?"
                                        />
                                    </FormGroup>
                                    <FormGroup>
                                        <Input
                                            required
                                            type="password"
                                            name="password"
                                            placeholder="password"
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
                                        Do not have an account? <a href="/signup">Click here</a>
                                    </p>
                                    <br /><br /><br /><br /><br /><br /><br /><br /><br />
                                </Form>
                            </CardBody>
                        </Card>

                    </Col>

                </Row>
            </div>
        );
    }

}

export default Login