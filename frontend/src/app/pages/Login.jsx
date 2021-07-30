import { useState } from 'react';
import { Card, CardHeader, CardBody, Col, Container, Row, CardColumns, CardFooter } from 'reactstrap';
import Service from '../service/Service'
import MAuth from '../model/MAuth'
import { Formik, Form, Field } from "formik";

export default function Login() {

    const [user, setUser] = useState({
        usernameOrEmail: '',
        password: '',
    })

    const onSubmit = (values) => {
        MAuth.login(values);
    }

    let { usernameOrEmail, password } = user;
    return (
        <div>
            <Row>
                <Col>
                    <Card>
                        <CardHeader>
                            <h3>Login</h3>
                        </CardHeader>
                        <CardBody>
                            <Formik
                                initialValues={{ usernameOrEmail, password }}
                                onSubmit={onSubmit}
                                validateOnChange={false}
                                validateOnBlur={false}
                                enableReinitialize={true}
                            >
                                <Form method="post">
                                    <fieldset className="form-group">
                                        <label htmlFor="usernameOrEmail" >Username or Email </label>
                                        <Field className="form-control" type="text" name="usernameOrEmail" id="usernameOrEmail" />
                                    </fieldset>
                                    <fieldset className="form-group ">
                                        <label>Password</label>
                                        <Field className="form-control" type="password" name="password" />
                                    </fieldset>
                                    <button className="btn btn-primary" type="submit">Login</button>
                                </Form>
                            </Formik>
                        </CardBody>
                        <CardFooter>
                            <p>Do not have an account yet? <a href="/signup">Register here!</a></p>
                        </CardFooter>
                    </Card>
                </Col>
            </Row>
        </div>
    );
}