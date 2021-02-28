import _ from 'lodash';
import React, { useEffect } from 'react';
import { Field, Formik } from "formik";
import { Col, Container, Row, Form } from "reactstrap";
import { ReactstrapInput } from "reactstrap-formik";

export default function Login() {
    useEffect(
        () => {
            
        }
    )

    const onSubmit = (values) => {
        console.log(values);
        //Make API calls here

        alert(
            `Submitted Successfully ->  ${JSON.stringify(values, null, 2)}`
        );
    }

    return (
        <Formik
            initialValues={{ email: "", password: "" }}
            onSubmit={onSubmit}
            render={({ submitForm, isSubmitting, values }) => (
                <Form>
                    <Container style={{ paddingTop: "5px" }}>
                        <Row>
                            <Col xs="12">
                                <Field
                                    type="email"
                                    label="Email"
                                    name="email"
                                    id="email"
                                    component={ReactstrapInput}
                                />
                            </Col>
                            <Col xs="12">
                                <Field
                                    type="password"
                                    label="Password"
                                    name="password"
                                    id="password"
                                    component={ReactstrapInput}
                                />
                            </Col>
                            <Col xs="12">
                                <button type="submit">Submit</button>
                            </Col>
                        </Row>
                        <pre>{JSON.stringify(values, null, 2)}</pre>
                    </Container>
                </Form>
            )}
        />
    );
}